/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.CoreMatchers.equalTo;
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
    objectStore = muleContext.getObjectStoreManager().createObjectStore(CUSTOM_STORE_NAME, unmanagedTransient());
  }

  @Test
  public void executeProtectedOperation() throws Exception {
    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);

    assertOAuthStateStored(CUSTOM_STORE_NAME, storedOwnerId, DEFAULT_RESOURCE_OWNER_ID);
  }

  @Test
  public void refreshToken() throws Exception {
    executeProtectedOperation();
    WireMock.reset();

    String refreshedToken = ACCESS_TOKEN + "-refreshed";
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
                                                                            .withStatus(OK.getStatusCode())
                                                                            .withBody(accessTokenContent(refreshedToken))
                                                                            .withHeader(CONTENT_TYPE, "application/json")));

    flowRunner("refreshToken").run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
    ResourceOwnerOAuthContext context = (ResourceOwnerOAuthContext) objectStore.retrieve(storedOwnerId);
    assertThat(context.getAccessToken(), equalTo(refreshedToken));
  }

  @Test
  public void unauthorize() throws Exception {
    executeProtectedOperation();

    flowRunner("unauthorize").run();
    ObjectStore objectStore = getObjectStore(CUSTOM_STORE_NAME);
    assertThat(objectStore.contains(storedOwnerId), is(false));
  }

  @Override
  protected void assertAuthCodeState(TestOAuthConnectionState connection) {
    ClientCredentialsState state = (ClientCredentialsState) connection.getState();
    assertThat(state.getAccessToken(), is(ACCESS_TOKEN));
    assertThat(state.getExpiresIn().get(), is(EXPIRES_IN));

  }
                   /*
  @Test
  public void refreshToken() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void callbackFlows() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      TestOAuthExtension config = getConfigurationFromRegistry("oauth", CoreEvent.builder(initialiserEvent)
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
  }

  protected void assertBeforeCallbackPayload(TestOAuthExtension config) {
    AuthCodeRequest request = config.getCapturedAuthCodeRequests().get(0);
    assertThat(request.getResourceOwnerId(), is(ownerId));
    assertScopes(request);
    assertThat(request.getState().get(), is(STATE));
    assertExternalCallbackUrl(request);
  }

  protected void assertScopes(AuthCodeRequest request) {
    assertThat(request.getScopes().get(), is(SCOPES));
  }

  private void assertAfterCallbackPayload(TestOAuthExtension config) {
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
  
                    */
}
