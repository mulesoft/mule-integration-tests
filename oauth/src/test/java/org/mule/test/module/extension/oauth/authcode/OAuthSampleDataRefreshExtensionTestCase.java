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

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.tomakehurst.wiremock.client.WireMock;

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

    Message message =
        sampleDataService.getSampleData(Location.builder().globalName("sampleData").addProcessorsPart().addIndexPart(0).build());
    assertThat(message.getPayload().getValue(), is(SAMPLE_PAYLOAD_VALUE));
    assertThat(message.getAttributes().getValue(), is(SAMPLE_ATTRIBUTES_VALUE));
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void tokenRefreshOnSampleDataResolutionThroughApi() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    Message message =
        sampleDataService.getSampleData(TEST_OAUTH_EXTENSION_NAME, "sampleDataOperation", emptyMap(),
                                        getConfigurationSupplier("oauth"));
    assertThat(message.getPayload().getValue(), is(SAMPLE_PAYLOAD_VALUE));
    assertThat(message.getAttributes().getValue(), is(SAMPLE_ATTRIBUTES_VALUE));
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  private Supplier<Optional<ConfigurationInstance>> getConfigurationSupplier(String configName) {
    if (configName == null) {
      return Optional::empty;
    }

    return (CheckedSupplier<Optional<ConfigurationInstance>>) () -> of(extensionManager.getConfiguration(configName,
                                                                                                         testEvent()));
  }

}
