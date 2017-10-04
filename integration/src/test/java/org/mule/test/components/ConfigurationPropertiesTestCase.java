/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class ConfigurationPropertiesTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/configuration-properties-config.xml";
  }

  @Description("Defines a configuration-properties with a file attribute that has a placeholder which value is provided by a global property")
  @Test
  public void configurationFileWithPlaceholdersDefinedWithGlobalProperty() {
    assertThat(configurationProperties.resolveStringProperty("myProp").get(), is("myEnvValue"));
  }

  @Description("Defines several scenarios of properties values which contains placeholders that needs to be resolved between global-property elements and configuration-properties files")
  @Test
  public void expandPropertiesWithOtherProperties() {
    assertThat(configurationProperties.resolveStringProperty("complexValue").get(), is("value2-value1"));
    assertThat(configurationProperties.resolveStringProperty("anotherComplexValue").get(), is("value1-filePropValue"));
    assertThat(configurationProperties.resolveStringProperty("http.path").get(), is("myEnvValue"));
    assertThat(configurationProperties.resolveStringProperty("http.url").get(), is("http://localhost/myEnvValue"));
  }
}
