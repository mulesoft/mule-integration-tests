/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.encodeQueryString;
import static org.mule.runtime.oauth.internal.OAuthConstants.ACCESS_TOKEN_PARAMETER;
import static org.mule.runtime.oauth.internal.OAuthConstants.EXPIRES_IN_PARAMETER;
import static org.mule.runtime.oauth.internal.OAuthConstants.REFRESH_TOKEN_PARAMETER;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.io.IOException;
import java.util.Map;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class BaseOAuthExtensionTestCase extends AbstractExtensionFunctionalTestCase {

  protected static final int REQUEST_TIMEOUT = 10000;
  protected static final String LOCAL_AUTH_PATH = "dance";
  protected static final String CALLBACK_PATH = "callback";
  protected static final String OWNER_ID_VARIABLE_NAME = "ownerId";
  protected static final String DEFAULT_OWNER_ID = "default";
  protected static final String TOKEN_PATH = "token";
  protected static final String STATE = "myState";
  protected static final String AUTHORIZE_PATH = "authorize";
  protected static final String USER_ID = "35";
  protected static final String INSTANCE_ID = "staging";
  protected static final String SCOPES = "this, that, those";
  protected static final String CONSUMER_KEY = "ndli93xdws2qoe6ms1d389vl6bxquv3e";
  protected static final String CONSUMER_SECRET = "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T";
  protected static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
  protected static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
  protected static final String EXPIRES_IN = "3897";
  protected static final String STATE_PARAMETER = "state";
  protected static final String CODE_PARAMETER = "code";
  protected static final String CUSTOM_STORE_NAME = "customStore";

  @Rule
  public SystemProperty consumerKey = new SystemProperty("consumerKey", CONSUMER_KEY);

  @Rule
  public SystemProperty consumerSecret = new SystemProperty("consumerSecret", CONSUMER_SECRET);

  @Rule
  public SystemProperty localAuthPath = new SystemProperty("localAuthPath", LOCAL_AUTH_PATH);

  @Rule
  public SystemProperty scope = new SystemProperty("scopes", SCOPES);

  @Rule
  public DynamicPort callbackPort = new DynamicPort("callbackPort");

  @Rule
  public SystemProperty callbackPath = new SystemProperty("callbackPath", CALLBACK_PATH);

  @Rule
  public DynamicPort oauthServerPort = new DynamicPort("oauthServerPort");

  @Rule
  public SystemProperty oauthProvider = new SystemProperty("callbackPath", CALLBACK_PATH);

  @Rule
  public WireMockRule wireMock = new WireMockRule(wireMockConfig()
      .bindAddress("127.0.0.1")
      .port(oauthServerPort.getNumber()));

  protected String authUrl = toUrl(AUTHORIZE_PATH, oauthServerPort.getNumber());

  @Rule
  public SystemProperty authorizationUrl = new SystemProperty("authorizationUrl", authUrl);


  protected String tokenUrl = toUrl(TOKEN_PATH, oauthServerPort.getNumber());

  @Rule
  public SystemProperty accessTokenUrl = new SystemProperty("accessTokenUrl", tokenUrl);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected String ownerId;
  protected String storedOwnerId;

  protected String toUrl(String path, int port) {
    return format("http://127.0.0.1:%d/%s", port, path);
  }

  protected void assertOAuthStateStored(String objectStoreName, String expectedKey, String expectedValue) throws Exception {
    ObjectStore objectStore = getObjectStore(objectStoreName);

    ResourceOwnerOAuthContext context = (ResourceOwnerOAuthContext) objectStore.retrieve(expectedKey);
    assertThat(context.getResourceOwnerId(), is(expectedValue));
  }

  protected ObjectStore getObjectStore(String objectStoreName) {
    ObjectStore objectStore = muleContext.getObjectStoreManager().getObjectStore(objectStoreName);
    assertThat(objectStore, is(notNullValue()));
    return objectStore;
  }

  protected void simulateDanceStart() throws IOException {
    simulateDanceStart(oauthServerPort.getNumber());
  }

  protected void simulateDanceStart(int port) throws IOException {
    wireMock.stubFor(get(urlMatching("/" + LOCAL_AUTH_PATH + ".*")).willReturn(aResponse().withStatus(OK.getStatusCode())));
    ImmutableMap.Builder<String, String> queryParamsBuilder = ImmutableMap.builder();
    if (ownerId != null) {
      queryParamsBuilder.put("resourceOwnerId", ownerId);
    }
    Map<String, String> queryParams = queryParamsBuilder
        .put("state", STATE)
        .build();

    String localAuthUrl = toUrl(LOCAL_AUTH_PATH, port);
    Get(localAuthUrl + "?" + encodeQueryString(queryParams)).addHeader("Connection", "close")
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();
  }

  protected void simulateCallback() {
    simulateCallback(callbackPort.getNumber());
  }

  protected void simulateCallback(int port) {
    final String authCode = "chu chu ua, chu chu ua";

    ImmutableMap.Builder<String, String> queryParamsBuilder = ImmutableMap.builder();
    if (ownerId != null) {
      queryParamsBuilder.put(STATE_PARAMETER, String.format("%s:resourceOwnerId=%s", STATE, ownerId));
    }

    Map<String, String> queryParams = queryParamsBuilder
        .put(CODE_PARAMETER, authCode)
        .build();

    stubTokenUrl(accessTokenContent());

    check(REQUEST_TIMEOUT, 500, () -> {
      Response response = Get(toUrl(CALLBACK_PATH, port) + "?" + encodeQueryString(queryParams)).addHeader("Connection", "close")
          .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

      assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(OK.getStatusCode()));
      return true;
    });
  }

  protected void stubTokenUrl(String responseContent) {
    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH)).willReturn(buildResponseContent(responseContent)));
  }

  protected ResponseDefinitionBuilder buildResponseContent(String responseContent) {
    return aResponse()
        .withStatus(OK.getStatusCode())
        .withBody(responseContent)
        .withHeader(CONTENT_TYPE, "application/json");
  }

  protected String accessTokenContent() {
    return accessTokenContent(ACCESS_TOKEN);
  }

  protected String accessTokenContent(String accessToken) {
    return "{" +
        "\"" + ACCESS_TOKEN_PARAMETER + "\":\"" + accessToken + "\"," +
        "\"" + EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
        "\"" + REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"," +
        "\"" + "id" + "\":\"" + USER_ID + "\"," +
        "\"" + "instance_url" + "\":\"" + INSTANCE_ID + "\"" +
        "}";
  }

  protected void verifyAuthUrlRequest() {
    verifyAuthUrlRequest(callbackPort.getNumber());
  }

  protected void verifyAuthUrlRequest(int port) {
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("redirect_uri", equalTo(toUrl(CALLBACK_PATH, port)))
        .withQueryParam("client_id", equalTo(CONSUMER_KEY))
        .withQueryParam("scope", equalTo(SCOPES.replaceAll(" ", "\\+")))
        .withQueryParam("state", containing(STATE)));
  }

  protected void assertConnectionState(TestOAuthConnectionState connection) {
    assertConnectionProperties(connection);
    assertAuthCodeState(connection);
  }

  protected String getRefreshTokenResponse() {
    return accessTokenContent(ACCESS_TOKEN + "-refreshed");
  }

  protected void assertAuthCodeState(TestOAuthConnectionState connection) {
    AuthorizationCodeState state = (AuthorizationCodeState) connection.getState();
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));
    assertThat(state.getRefreshToken().get(), is(REFRESH_TOKEN));
    assertThat(state.getState().get(), is(STATE));
    assertThat(state.getResourceOwnerId(), is(ownerId));
  }

  protected void assertConnectionProperties(TestOAuthConnectionState connection) {
    assertThat(connection, is(notNullValue()));
    assertThat(connection.getApiVersion(), is(34.0D));
    assertThat(connection.getDisplay(), is("PAGE"));
    assertThat(connection.isPrompt(), is(false));
    assertThat(connection.isImmediate(), is(true));
    assertThat(connection.getInstanceId(), is(INSTANCE_ID));
    assertThat(connection.getUserId(), is(USER_ID));
  }

  protected void stubRefreshedTokenAlreadyExpired() {
    WireMock.reset();

    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH))
        .inScenario("refreshTokenWasAlreadyExpired")
        .whenScenarioStateIs(STARTED)
        .willReturn(buildResponseContent(accessTokenContent(ACCESS_TOKEN)))
        .willSetStateTo("refresh"));

    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH))
        .inScenario("refreshTokenWasAlreadyExpired")
        .whenScenarioStateIs("refresh")
        .willReturn(buildResponseContent(getRefreshTokenResponse())));
  }

  protected void stubRefreshedTokenAlreadyExpiredTwice() {
    WireMock.reset();

    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH))
        .inScenario("refreshTokenWasAlreadyExpiredTwice")
        .whenScenarioStateIs(STARTED)
        .willReturn(buildResponseContent(accessTokenContent(ACCESS_TOKEN)))
        .willSetStateTo("2"));

    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH))
        .inScenario("refreshTokenWasAlreadyExpiredTwice")
        .whenScenarioStateIs("2")
        .willReturn(buildResponseContent(accessTokenContent(ACCESS_TOKEN)))
        .willSetStateTo("refresh"));

    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH))
        .inScenario("refreshTokenWasAlreadyExpiredTwice")
        .whenScenarioStateIs("refresh")
        .willReturn(buildResponseContent(getRefreshTokenResponse())));
  }

  protected void stubRefreshToken() {
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));
  }

  protected String getCustomOwnerId() {
    return "MG";
  }

  protected void verifyTokenRefreshedTwice() {
    wireMock.verify(2, postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  protected void expectExpiredTokenException() {
    expectedException.expectMessage(getExpirationMessageSubstring());
  }

  protected String getExpirationMessageSubstring() {
    return "Access Token expired for resource owner id";
  }
}
