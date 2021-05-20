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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@RunnerDelegateTo(Parameterized.class)
public class LocalisationPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String> configs() {
    return asList("properties/localisation-properties-resolver-extension-config.xml");
  }

  @Inject
  private ConfigurationProperties configurationProperties;

  private final String configFile;

  public LocalisationPropertiesResolverExtensionTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void propertiesAreResolvedCorrectlyAndNumberIsFormattedAccordingToLocale() {
    NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "PT"));
    DecimalFormat formatter = (DecimalFormat) nf;
    formatter.applyPattern("#,##0.00");
    String expectedResult = formatter.format(Double.parseDouble("25837889.4535"));
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is(expectedResult));
  }
}

