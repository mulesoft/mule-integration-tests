/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import static java.lang.String.format;
import static java.util.Collections.synchronizedList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

//public class OAuthHandlerIntegrationHandler extends MuleArtifactFunctionalTestCase {
public class OAuthHandlerObjectStoreTestCase extends BaseOAuthExtensionTestCase {

  private static List<Object> payloads;
  private static final int HTTP_STATUS_OK = 200;
  public static final int RESPONSE_TIMEOUT = 5000;


  /*
   * @Rule public ExpectedException expectedException = ExpectedException.none(); protected String authUrl = toUrl(AUTHORIZE_PATH,
   * oauthServerPort.getNumber());
   * 
   * @Rule public SystemProperty authorizationUrl = new SystemProperty("authorizationUrl", authUrl);
   * 
   * protected String tokenUrl = toUrl(TOKEN_PATH, oauthServerPort.getNumber());
   * 
   * @Rule public SystemProperty accessTokenUrl = new SystemProperty("accessTokenUrl", tokenUrl);
   * 
   * protected String ownerId;
   */

  @Override
  protected String toUrl(String path, int port) {
    String url = format("http://127.0.0.1:%d/%s", port, path);
    System.out.println("url:" + url);
    return url;
  }

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /*
   * @Rule public DynamicPort port = new DynamicPort("port");
   */
  public String port = "8081";

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    // objectStore.get().clear();
    payloads = synchronizedList(new ArrayList<>());
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
        .withStatus(OK.getStatusCode())
        .withBody(accessTokenContent())
        .withHeader(CONTENT_TYPE, "application/json")));
  }

  /*
   * @Override protected void doSetUp() throws Exception { super.doSetUp(); payloads = synchronizedList(new ArrayList<>()); }
   */

  @Override
  /*
   * protected String getConfigFile() { return "oauthHandlerWithObjectStore.xml"; }
   */
  /*
   * protected String[] getConfigFiles() { return new String[] {"oauthHandlerWithObjectStore.xml",
   * "oauthHandlerWithObjectStoreFlows.xml"}; }
   */
  protected String[] getConfigFiles() {
    // return new String[] {"oauthHandlerWithObjectStoreFlows.xml", "oauthHandlerWithObjectStore.xml"};
    return new String[] {"oauthHandlerWithObjectStoreFlows.xml"};
  }

  @Test
  @Issue("W-11493901")
  @Description("verifiy object store is created if not found")
  public void execute() throws Exception {

    /*
     * // HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port.getNumber() + "/createClientz").method(GET)
     * // Create client HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port + "/createClient").method(POST)
     * .addHeader("client_id", CONSUMER_KEY) .addHeader("client_secret", CONSUMER_SECRET) .addHeader("client_name",
     * "River products") .build();
     *
     * HttpResponse response = httpClient.send(request, HttpRequestOptions.builder().responseTimeout(RESPONSE_TIMEOUT).build());
     * assertThat(response.getStatusCode(), is(HTTP_STATUS_OK)); String content =
     * IOUtils.toString(response.getEntity().getContent()); System.out.println("Content:" + content);
     *
     * System.setProperty("tokenUrl", "http://localhost:" + port + "/token"); System.setProperty("clientId", "abc8");
     * System.setProperty("clientSecret", "abcdefg");
     */
    // AnotherClass anotherObjSpy = Mockito.spy(new AnotherClass());


    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();
    System.out.println(connection.getConnectionDetails().toString());
    ObjectStore os = getObjectStore(CUSTOM_STORE_NAME);
    assertThat(os, notNullValue());
    /*
     * HttpRequest requestToken = HttpRequest.builder().uri("http://localhost:" + port + "/token").method(POST)
     * .addHeader("client_id", "abc8") .addHeader("client_secret", "abcdefg") .addHeader("grant_type", "CLIENT_CREDENTIALS")
     * .build(); HttpResponse responseToken = httpClient.send(requestToken,
     * HttpRequestOptions.builder().responseTimeout(RESPONSE_TIMEOUT).build()); assertThat(responseToken.getStatusCode(),
     * is(HTTP_STATUS_OK)); content = IOUtils.toString(responseToken.getEntity().getContent()); JSONObject jsonResponse = new
     * JSONObject(content); String token = (String) jsonResponse.get("access_token"); System.out.println("Token Content" + token);
     */

    /*
     * HttpRequest requestValidate = HttpRequest.builder().uri("http://localhost:" + port + "/validate").method(POST)
     * .addHeader("Authorization", "Bearer " + token) .build(); HttpResponse responseValidate = httpClient.send(requestValidate,
     * HttpRequestOptions.builder().responseTimeout(RESPONSE_TIMEOUT).build()); assertThat(responseToken.getStatusCode(),
     * is(HTTP_STATUS_OK));
     */

  }

  private static final String getBasicAuthenticationHeader(String username, String password) {
    String valueToEncode = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }
}
