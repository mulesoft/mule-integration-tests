/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@RunnerDelegateTo(Parameterized.class)
public class CustomPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Parameters(name = "{0}")
  public static Collection<String> configs() {
    return asList("properties/custom-properties-resolver-extension-config.xml",
                  "properties/custom-properties-resolver-extension-deprecated-config.xml");
  }

  @Inject
  private ConfigurationProperties configurationProperties;

  private final String configFile;

  public CustomPropertiesResolverExtensionTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    // Also check that provider is disposed
    assertThat(configurationProperties.resolveStringProperty("lifecycle::initialize").get(), is("1"));
    assertThat(configurationProperties.resolveStringProperty("lifecycle::dispose").get(), is("1"));
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void propertiesAreResolvedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is("test.key1:value1:AES:CBC"));
    assertThat(configurationProperties.resolveStringProperty("key2").get(), is("test.key2:value2:AES:CBC"));
  }

  @Test
  public void providerIsInitialisedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("lifecycle::initialize").get(), is("1"));
    assertThat(configurationProperties.resolveStringProperty("lifecycle::dispose").get(), is("0"));
  }

}
