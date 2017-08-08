/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;
import org.mule.test.oauth.TestOAuthExtension;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Before;
import org.junit.Test;

public class OAuthExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"dynamic-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = CUSTOM_OWNER_ID;
    storedOwnerId = CUSTOM_OWNER_ID + "-oauth";
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
        .withVariable(OWNER_ID_VARIABLE_NAME, CUSTOM_OWNER_ID)
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);
    assertExternalCallbackUrl(connection.getState());

    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, storedOwnerId, ownerId);
  }

  @Test
  public void refreshToken() throws Exception {
    receiveAccessTokenAndUserConnection();
    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, CUSTOM_OWNER_ID).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void callbackFlows() throws Exception {
    authorizeAndStartDancingBaby();
    receiveAccessTokenAndUserConnection();

    TestOAuthExtension config = getConfigurationFromRegistry("oauth", Event.builder(getInitialiserEvent())
        .addVariable(OWNER_ID_VARIABLE_NAME, CUSTOM_OWNER_ID)
        .build(), muleContext);

    check(REQUEST_TIMEOUT, 500, () -> {
      assertThat(config.getCapturedAuthCodeRequests(), hasSize(1));
      assertThat(config.getCapturedAuthCodeStates(), hasSize(1));
      return true;
    });

    assertBeforeCallbackPayload(config);
    assertAfterCallbackPayload(config);
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
}
