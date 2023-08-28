/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import static org.mule.oauth.client.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.oauth.client.internal.OAuthConstants.CLIENT_SECRET_PARAMETER;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

public class OAuthExtensionWithCredentialsPlacementTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public DynamicPort otherCallbackPort = new DynamicPort("otherCallbackPort");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-with-credentials-placement-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void testCredentialsPlacementInHeaderForTokenRequest() {
    simulateCallback(callbackPort.getNumber());
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withHeader(AUTHORIZATION, equalTo(toBasicAuthorizationHeader(CONSUMER_KEY, CONSUMER_SECRET)))
        .withRequestBody(notMatching(".*" + CLIENT_ID_PARAMETER + ".*"))
        .withRequestBody(notMatching(".*" + CLIENT_SECRET_PARAMETER + ".*")));
  }

  @Test
  public void testCredentialsPlacementInHeaderForRefreshTokenRequest() throws Exception {
    simulateCallback(callbackPort.getNumber());
    WireMock.reset();
    stubRefreshToken();
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withHeader(AUTHORIZATION, equalTo(toBasicAuthorizationHeader(CONSUMER_KEY, CONSUMER_SECRET)))
        .withRequestBody(notMatching(".*" + CLIENT_ID_PARAMETER + ".*"))
        .withRequestBody(notMatching(".*" + CLIENT_SECRET_PARAMETER + ".*"))
        .withRequestBody(containing(REFRESH_TOKEN)));
  }

  @Test
  public void testCredentialsPlacementDefaultsToBodyForTokenRequest() {
    simulateCallback(otherCallbackPort.getNumber());
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withoutHeader(AUTHORIZATION)
        .withRequestBody(matching(".*" + CLIENT_ID_PARAMETER + ".*"))
        .withRequestBody(matching(".*" + CLIENT_SECRET_PARAMETER + ".*")));
  }

  @Test
  public void testCredentialsPlacementDefaultsToBodyForRefreshTokenRequest() throws Exception {
    simulateCallback(otherCallbackPort.getNumber());
    WireMock.reset();
    stubRefreshToken();
    flowRunner("refreshOtherToken").withVariable(OWNER_ID_VARIABLE_NAME, getCustomOwnerId()).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH))
        .withoutHeader(AUTHORIZATION)
        .withRequestBody(matching(".*" + CLIENT_ID_PARAMETER + ".*"))
        .withRequestBody(matching(".*" + CLIENT_SECRET_PARAMETER + ".*"))
        .withRequestBody(containing(REFRESH_TOKEN)));
  }
}
