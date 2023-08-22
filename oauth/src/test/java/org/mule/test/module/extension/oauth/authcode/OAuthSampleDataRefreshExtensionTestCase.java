/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.OauthFeature.SDK_OAUTH_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SampleDataStory.SAMPLE_DATA_SERVICE;
import static org.mule.test.oauth.RefreshedOAuthSampleDataProvider.SAMPLE_ATTRIBUTES_VALUE;
import static org.mule.test.oauth.RefreshedOAuthSampleDataProvider.SAMPLE_PAYLOAD_VALUE;
import static org.mule.test.oauth.TestOAuthExtension.TEST_OAUTH_EXTENSION_NAME;

import static java.util.Collections.emptyMap;
import static java.util.Optional.of;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(SDK_OAUTH_SUPPORT), @Feature(SDK_TOOLING_SUPPORT)})
@Story(SAMPLE_DATA_SERVICE)
public class OAuthSampleDataRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-static-config.xml", "oauth-extension-flows.xml"};
  }

  @Inject
  @Named(SAMPLE_DATA_SERVICE_KEY)
  private SampleDataService sampleDataService;

  @Inject
  private ExtensionManager extensionManager;

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void tokenRefreshOnSampleDataResolutionByLocation() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    assertSampleData();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshedTokenExpiredOnSampleDataResolutionByLocation() throws Exception {
    simulateCallback();
    stubRefreshedTokenAlreadyExpired();
    assertSampleData();
    verifyTokenRefreshedTwice();
  }

  @Test
  public void refreshedTokenExpiredTwiceOnSampleDataResolutionByLocation() throws Exception {
    simulateCallback();
    stubRefreshedTokenAlreadyExpiredTwice();
    expectExpiredTokenException();

    try {
      getSampleData();
    } finally {
      verifyTokenRefreshedTwice();
    }
  }

  @Test
  public void tokenRefreshOnSampleDataResolutionThroughApi() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    Message message =
        sampleDataService.getSampleData(TEST_OAUTH_EXTENSION_NAME, "sampleDataOperation", emptyMap(),
                                        getConfigurationSupplier("oauth"));
    assertSampleData(message);
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshedTokenAlreadyExpiredOnSampleDataResolutionThroughApi() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpired();

    Message message =
        sampleDataService.getSampleData(TEST_OAUTH_EXTENSION_NAME, "sampleDataOperation", emptyMap(),
                                        getConfigurationSupplier("oauth"));
    assertSampleData(message);
    verifyTokenRefreshedTwice();
  }

  @Test
  public void refreshedTokenAlreadyExpiredTwiceOnSampleDataResolutionThroughApi() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpiredTwice();
    expectExpiredTokenException();

    Message message =
        sampleDataService.getSampleData(TEST_OAUTH_EXTENSION_NAME, "sampleDataOperation", emptyMap(),
                                        getConfigurationSupplier("oauth"));

    try {
      assertSampleData(message);
    } finally {
      verifyTokenRefreshedTwice();
    }
  }

  private Supplier<Optional<ConfigurationInstance>> getConfigurationSupplier(String configName) {
    if (configName == null) {
      return Optional::empty;
    }

    return (CheckedSupplier<Optional<ConfigurationInstance>>) () -> of(extensionManager.getConfiguration(configName,
                                                                                                         testEvent()));
  }

  private void assertSampleData() throws SampleDataException {
    Message message = getSampleData();
    assertSampleData(message);
  }

  private void assertSampleData(Message message) {
    assertThat(message.getPayload().getValue(), is(SAMPLE_PAYLOAD_VALUE));
    assertThat(message.getAttributes().getValue(), is(SAMPLE_ATTRIBUTES_VALUE));
  }

  private Message getSampleData() throws SampleDataException {
    return sampleDataService.getSampleData(Location.builder()
        .globalName("sampleData").addProcessorsPart().addIndexPart(0)
        .build());
  }
}
