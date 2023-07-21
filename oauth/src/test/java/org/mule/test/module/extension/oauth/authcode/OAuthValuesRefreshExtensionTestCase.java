/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.OauthFeature.SDK_OAUTH_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ValueProvidersStory.VALUE_PROVIDERS_SERVICE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(SDK_OAUTH_SUPPORT), @Feature(SDK_TOOLING_SUPPORT)})
@Story(VALUE_PROVIDERS_SERVICE)
public class OAuthValuesRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-static-config.xml", "oauth-extension-flows.xml"};
  }

  @Inject
  @Named(VALUE_PROVIDER_SERVICE_KEY)
  protected ValueProviderService valueProviderService;

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void tokenRefreshOnValuesResolution() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    assertValues();
  }

  @Test
  public void refreshedTokenAlreadyExpiredOnValuesResolution() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpired();
    assertValues();
    verifyTokenRefreshedTwice();
  }

  @Test
  public void refreshedTokenAlreadyExpiredTwiceOnValuesResolution() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpiredTwice();

    ValueResult valueResult = getValues();
    assertThat(valueResult.isSuccess(), is(false));
    assertThat(valueResult.getFailure().get().getMessage(), containsString(getExpirationMessageSubstring()));
    verifyTokenRefreshedTwice();
  }

  private void assertValues() {
    ValueResult valueResult = getValues();
    assertThat(valueResult.isSuccess(), is(true));
  }

  private ValueResult getValues() {
    return valueProviderService.getValues(Location.builder()
        .globalName("values")
        .addProcessorsPart()
        .addIndexPart(0)
        .build(),
                                          "parameter");
  }
}
