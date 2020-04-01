/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import io.qameta.allure.Description;
import org.junit.Test;

public class DynamicOAuthClientCredentialsExtensionTestCase extends BaseOAuthExtensionTestCase {

  private static final String CLIENT_ID = "dynamicClientId";
  private static final String CLIENT_SECRET = "dynamicClientSecret";
  private static final String SCOPES = "dynamic scopes list";

  private static final String ANOTHER_CLIENT_ID = "anotherDynamicClientId";
  private static final String ANOTHER_CLIENT_SECRET = "anotherDynamicClientSecret";
  private static final String ANOTHER_SCOPES = "another scope set";

  private static final String CLIENT_ID_VARIABLE = "clientId";
  private static final String CLIENT_SECRET_VARIABLE = "clientSecret";
  private static final String TOKEN_URL_VARIABLE = "tokenUrl";
  private static final String SCOPES_VARIABLE = "scopes";

  private LazyValue<ObjectStore> objectStore =
      new LazyValue<>(() -> muleContext.getObjectStoreManager().getObjectStore(CUSTOM_STORE_NAME));

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"dynamic-client-credentials-oauth-extension-config.xml", "client-credentials-flows.xml"};
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    storedOwnerId = DEFAULT_RESOURCE_OWNER_ID + "-oauth";
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
        .withStatus(OK.getStatusCode())
        .withBody(accessTokenContent())
        .withHeader(CONTENT_TYPE, "application/json")));
  }

  @Test
  @Description("Use different values for client credentials paramenter and check that those values are used.")
  public void getConnectionWithDynamicConfig() throws Exception {
    getConnectionFlow(CLIENT_ID, CLIENT_SECRET, SCOPES);
    verifyCall(CLIENT_ID, CLIENT_SECRET, SCOPES);
  }

  @Test
  @Description("Create two dynamic configuration with different values and check the second values are used for the dance.")
  public void getSecondConnectionwithDynamicConfig() throws Exception {
    getConnectionFlow(CLIENT_ID, CLIENT_SECRET, SCOPES);
    getConnectionFlow(ANOTHER_CLIENT_ID, ANOTHER_CLIENT_SECRET, ANOTHER_SCOPES);
    verifyCall(ANOTHER_CLIENT_ID, ANOTHER_CLIENT_SECRET, ANOTHER_SCOPES);
  }

  private void getConnectionFlow(String clientId, String clientSecret, String scopes) throws Exception {
    flowRunner("getConnection")
        .withVariable(TOKEN_URL_VARIABLE, tokenUrl)
        .withVariable(CLIENT_ID_VARIABLE, clientId)
        .withVariable(CLIENT_SECRET_VARIABLE, clientSecret)
        .withVariable(SCOPES_VARIABLE, scopes)
        .run();
  }

  private void verifyCall(String clientId, String clientSecret, String scopes) {
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withQueryParam("client_id", equalTo(clientId))
        .withQueryParam("client_secret", equalTo(clientSecret))
        .withRequestBody(containing(scopes.replaceAll(" ", "\\+"))));
  }

}
