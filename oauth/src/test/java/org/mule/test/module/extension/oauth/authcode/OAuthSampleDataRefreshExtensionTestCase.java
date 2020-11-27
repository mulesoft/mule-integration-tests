/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.test.oauth.RefreshedOAuthSampleDataProvider.SAMPLE_ATTRIBUTES_VALUE;
import static org.mule.test.oauth.RefreshedOAuthSampleDataProvider.SAMPLE_PAYLOAD_VALUE;
import static org.mule.test.oauth.TestOAuthExtension.TEST_OAUTH_EXTENSION_NAME;

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

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
    storedOwnerId = getCustomOwnerId() + "-oauth";
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
