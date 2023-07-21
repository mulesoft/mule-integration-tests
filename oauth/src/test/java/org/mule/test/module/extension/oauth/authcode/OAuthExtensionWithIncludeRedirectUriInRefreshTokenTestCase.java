/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

public class OAuthExtensionWithIncludeRedirectUriInRefreshTokenTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public DynamicPort otherCallbackPort = new DynamicPort("otherCallbackPort");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-with-include-redirect-uri-in-refresh-token-config.xml",
        "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void testRedirectUriParamIsIncludedForTokenRequest() {
    simulateCallback(callbackPort.getNumber());
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withRequestBody(containing(REDIRECT_URI)));
  }

  @Test
  public void testRedirectUriParamIsNotIncludedForRefreshTokenRequest() throws Exception {
    simulateCallback(callbackPort.getNumber());
    WireMock.reset();
    stubRefreshToken();
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withRequestBody(notMatching(".*" + REDIRECT_URI + ".*")));
  }

  @Test
  public void testRedirectUriParamIsIncludedByDefaultForRefreshTokenRequest() throws Exception {
    simulateCallback(otherCallbackPort.getNumber());
    WireMock.reset();
    stubRefreshToken();
    flowRunner("refreshOtherToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withRequestBody(containing(REDIRECT_URI)));
  }
}
