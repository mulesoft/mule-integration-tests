/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.connectivity.oauth.AuthCodeRequest;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.tck.junit4.rule.SystemProperty;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;

public class OAuthExtensionWithExternalUrlTestCase extends OAuthExtensionTestCase {

  private static final String EXTERNAL_CALLBACK_URL = "https://my.proxied.callback.mg.com";

  @Rule
  public SystemProperty externalCallbackUrl = new SystemProperty("externalCallbackUrl", EXTERNAL_CALLBACK_URL);

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"oauth-extension-with-custom-external-url-config.xml", "oauth-extension-flows.xml"};
  }

  protected void verifyAuthUrlRequest() {
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("redirect_uri", equalTo(EXTERNAL_CALLBACK_URL))
        .withQueryParam("client_id", equalTo(CONSUMER_KEY))
        .withQueryParam("scope", equalTo(SCOPES.replaceAll(" ", "\\+")))
        .withQueryParam("state", containing(STATE)));
  }

  @Override
  protected void assertExternalCallbackUrl(AuthCodeRequest request) {
    assertThat(request.getExternalCallbackUrl().get(), CoreMatchers.equalTo(EXTERNAL_CALLBACK_URL));
  }

  @Override
  protected void assertExternalCallbackUrl(AuthorizationCodeState state) {
    assertThat(state.getExternalCallbackUrl().get(), CoreMatchers.equalTo(EXTERNAL_CALLBACK_URL));
  }

  @Override
  protected void assertExternalCallbackUrlOnAfterCallback(AuthorizationCodeState state) {
    assertThat(state.getExternalCallbackUrl().get(), CoreMatchers.equalTo(EXTERNAL_CALLBACK_URL));
  }
}
