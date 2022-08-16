/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import com.github.tomakehurst.wiremock.client.WireMock;

public class OAuthValuesRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-static-config.xml", "oauth-extension-flows.xml"};
  }

  @Inject
  @Named(VALUE_PROVIDER_SERVICE_KEY)
  protected ValueProviderService valueProviderService;

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void tokenRefreshOnValuesResolution() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    ValueResult valueResult = valueProviderService
        .getValues(Location.builder().globalName("values").addProcessorsPart().addIndexPart(0).build(),
                   "parameter");
    assertThat(valueResult.isSuccess(), is(true));
  }

}
