/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.AuthCodeConfig;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OAuthExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
    storedOwnerId = getCustomOwnerId() + "-oauth";
  }

  @Test
  public void authorizeAndStartDancingBaby() throws Exception {
    simulateDanceStart();
    verifyAuthUrlRequest();
  }

  @Test
  public void receiveAccessTokenAndUserConnection() throws Exception {
    simulateCallback();

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId())
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);
    assertExternalCallbackUrl((AuthorizationCodeState) connection.getState());

    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, storedOwnerId, ownerId);
  }

  @Test
  public void refreshToken() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshTokenWasAlreadyExpired() throws Exception {
    receiveAccessTokenAndUserConnection();
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

    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(2, postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshTokenWasAlreadyExpiredTwice() throws Exception {
    receiveAccessTokenAndUserConnection();
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

    expectedException.expectMessage("Access Token expired for resource owner id MG");

    try {
      flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    } finally {
      wireMock.verify(2, postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
    }
  }

  @Test
  public void refreshTokenForPagedOperationOnFirstPage() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(getRefreshTokenResponse());
    flowRunner("pagedOperationFailsAtFirstPage").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshTokenForPagedOperationOnThirdPage() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(getRefreshTokenResponse());
    CoreEvent event = flowRunner("pagedOperationFailsAtThirdPage")
        .withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId())
        .run();

    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
    List<String> accumulator = (List<String>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, hasSize(3));
    assertThat(accumulator, Matchers.contains("item 1", "item 2", "item 3"));
  }

  @Test
  public void callbackFlows() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      AuthCodeConfig config = getConfigurationFromRegistry("oauth", CoreEvent.builder(initialiserEvent)
          .addVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId())
          .build(), muleContext);

      check(REQUEST_TIMEOUT, 500, () -> {
        assertThat(config.getCapturedAuthCodeRequests(), hasSize(1));
        assertThat(config.getCapturedAuthCodeStates(), hasSize(1));
        return true;
      });

      assertBeforeCallbackPayload(config);
      assertAfterCallbackPayload(config);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  @Test
  public void unauthorize() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    flowRunner("unauthorize").withVariable(OWNER_ID_VARIABLE_NAME, ownerId).run();
    ObjectStore objectStore = getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY);
    assertThat(objectStore.contains(storedOwnerId), is(false));

    if (expectedError != null) {
      expectedError.expectErrorType("TEST-OAUTH", MuleErrors.CONNECTIVITY.name());
    }

    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
  }

  @Test
  public void reauthorize() throws Exception {
    expectedError = null;
    try {
      unauthorize();
    } catch (Exception e) {
      assertThat(e.getCause().getClass().getName(), containsString("TokenInvalidatedException"));
    }

    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();
  }

  protected void assertBeforeCallbackPayload(AuthCodeConfig config) {
    AuthCodeRequest request = config.getCapturedAuthCodeRequests().get(0);
    assertThat(request.getResourceOwnerId(), is(ownerId));
    assertScopes(request);
    assertThat(request.getState().get(), is(STATE));
    assertExternalCallbackUrl(request);
  }

  protected void assertScopes(AuthCodeRequest request) {
    assertThat(request.getScopes().get(), is(SCOPES));
  }

  private void assertAfterCallbackPayload(AuthCodeConfig config) {
    AuthorizationCodeState state = config.getCapturedAuthCodeStates().get(0);
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getRefreshToken().get(), is(REFRESH_TOKEN));
    assertThat(state.getResourceOwnerId(), is(ownerId));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));
    assertThat(state.getState().get(), is(STATE));
    assertThat(state.getAuthorizationUrl(), is(authUrl));
    assertThat(state.getAccessTokenUrl(), is(tokenUrl));
    assertThat(state.getConsumerKey(), is(CONSUMER_KEY));
    assertThat(state.getConsumerSecret(), is(CONSUMER_SECRET));
    assertExternalCallbackUrlOnAfterCallback(state);
  }

  protected void assertExternalCallbackUrl(AuthCodeRequest request) {
    assertThat(request.getExternalCallbackUrl().isPresent(), is(false));
  }

  protected void assertExternalCallbackUrlOnAfterCallback(AuthorizationCodeState state) {
    assertThat(state.getExternalCallbackUrl().isPresent(), is(false));
  }

  protected void assertExternalCallbackUrl(AuthorizationCodeState state) {
    assertThat(state.getExternalCallbackUrl().isPresent(), is(false));
  }
}
