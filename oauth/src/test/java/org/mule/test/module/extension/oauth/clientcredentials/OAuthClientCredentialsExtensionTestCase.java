/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedTransient;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class OAuthClientCredentialsExtensionTestCase extends BaseOAuthExtensionTestCase {

  private ObjectStore objectStore;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-extension-config.xml", "client-credentials-flows.xml"};
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    storedOwnerId = DEFAULT_RESOURCE_OWNER_ID + "-oauth";
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
                                                                            .withStatus(OK.getStatusCode())
                                                                            .withBody(accessTokenContent())
                                                                            .withHeader(CONTENT_TYPE, "application/json")));

    // This needs to be created before the app starts, since the token will be refreshed during startup and that needs the OS
    // created.
    objectStore = muleContext.getObjectStoreManager().createObjectStore(CUSTOM_STORE_NAME, unmanagedTransient());

    muleContext.start();
  }

  @Override
  protected boolean isStartContext() {
    return false;
  }

  @Test
  public void authenticate() throws Exception {
    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);

    assertOAuthStateStored(CUSTOM_STORE_NAME, storedOwnerId, DEFAULT_RESOURCE_OWNER_ID);
  }

  @Test
  public void refreshToken() throws Exception {
    authenticate();
    WireMock.reset();

    String refreshedToken = configureRefreshResponse();
    flowRunner("refreshToken").run();
    assertRefreshToken(refreshedToken);
  }

  private void assertRefreshToken(String refreshedToken) throws Exception {
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
    ResourceOwnerOAuthContext context = (ResourceOwnerOAuthContext) objectStore.retrieve(storedOwnerId);
    assertThat(context.getAccessToken(), CoreMatchers.equalTo(refreshedToken));
  }

  private String configureRefreshResponse() {
    String refreshedToken = ACCESS_TOKEN + "-refreshed";
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
                                                                            .withStatus(OK.getStatusCode())
                                                                            .withBody(accessTokenContent(refreshedToken))
                                                                            .withHeader(CONTENT_TYPE, "application/json")));
    return refreshedToken;
  }

  @Test
  public void unauthorize() throws Exception {
    authenticate();

    flowRunner("unauthorize").run();
    ObjectStore objectStore = getObjectStore(CUSTOM_STORE_NAME);
    assertThat(objectStore.contains(storedOwnerId), is(false));

    String refreshedToken = configureRefreshResponse();
    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertThat(connection.getState().getAccessToken(), CoreMatchers.equalTo(refreshedToken));
    assertRefreshToken(refreshedToken);
  }

  @Override
  protected void assertAuthCodeState(TestOAuthConnectionState connection) {
    ClientCredentialsState state = (ClientCredentialsState) connection.getState();
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));
  }

  @Test
  public void authenticateWithCustomParameters() throws Exception {
    WireMock.reset();
    storedOwnerId = DEFAULT_RESOURCE_OWNER_ID + "-customParametersOAuth";
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH))
                         .withHeader("foo", equalTo("bar"))
                         .withHeader("foo", equalTo("manchu"))
                         .withQueryParam("immediate", equalTo("true"))
                         .withQueryParam("prompt", equalTo("false"))
                         .withHeader("knownCustomHeader", equalTo("myHeader"))
                         .willReturn(aResponse()
                                         .withStatus(OK.getStatusCode())
                                         .withBody(accessTokenContent())
                                         .withHeader(CONTENT_TYPE, "application/json")));

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnectionWithCustomParameters")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);
    assertOAuthStateStored(CUSTOM_STORE_NAME, storedOwnerId, DEFAULT_RESOURCE_OWNER_ID);
  }
}
