/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Before;
import org.junit.Test;

public class OAuthQueryParamParameterExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-with-parameter-static-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void aliasedOAuthParameter() throws Exception {
    simulateDanceStart();
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("with_alias", equalTo("withAlias")));
  }

  @Test
  public void defaultPlacementParameter() throws Exception {
    simulateDanceStart();
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("defaultPlacement", equalTo("defaultPlacement")));
  }

  @Test
  public void explicitPlacementParameter() throws Exception {
    simulateDanceStart();
    wireMock.verify(getRequestedFor(urlPathEqualTo("/" + AUTHORIZE_PATH))
        .withQueryParam("queryParam", equalTo("queryParam")));
  }

}
