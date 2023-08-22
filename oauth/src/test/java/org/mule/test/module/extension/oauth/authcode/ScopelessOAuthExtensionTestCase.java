/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.sdk.api.connectivity.oauth.AuthCodeRequest;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.oauth.TestWithOAuthParamsConnectionProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ScopelessOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Rule
  public DynamicPort otherCallbackPort = new DynamicPort("otherCallbackPort");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"scopeless-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Override
  protected void assertScopes(AuthCodeRequest request) {
    assertThat(request.getScopes().isPresent(), is(false));
  }

  @Before
  public void setOwnerId() {
    ownerId = getCustomOwnerId();
  }

  @Override
  protected void verifyAuthUrlRequest() {
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("redirect_uri", equalTo((toUrl(CALLBACK_PATH, callbackPort.getNumber()))))
        .withQueryParam("client_id", equalTo(CONSUMER_KEY))
        .withQueryParam("state", containing(STATE)));
  }

  @Test
  public void testDefaultScopesOnDifferentConnectionProvidersAreHonored() throws Exception {
    simulateDanceStart(callbackPort.getNumber());
    verifyAuthUrlRequest(callbackPort.getNumber(), null);

    simulateDanceStart(otherCallbackPort.getNumber());
    verifyAuthUrlRequest(otherCallbackPort.getNumber(), TestWithOAuthParamsConnectionProvider.DEFAULT_SCOPE);
  }
}
