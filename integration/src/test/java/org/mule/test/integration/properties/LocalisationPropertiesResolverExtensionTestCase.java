/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static java.lang.Double.parseDouble;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class LocalisationPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ConfigurationProperties configurationProperties;

  private final String configFile;

  public LocalisationPropertiesResolverExtensionTestCase() {
    this.configFile = "properties/localisation-properties-resolver-extension-config.xml";
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  @Issue("MULE-19428")
  @Description("Verifies that given a locale and a pattern, a certain number can be formatted correctly using a properties resolver extension")
  public void propertiesAreResolvedCorrectlyAndNumberIsFormattedAccordingToLocale() {
    // The locale is given because DecimalFormat requires one to match the pattern with the desired region.
    // If a locale is not specified, DecimalFormat formats the number according to the english way
    DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("pt", "PT"));
    // The pattern is given to imitate the annotated issue as much as possible
    formatter.applyPattern("#,##0.00");
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is(formatter.format(parseDouble("25837889.4535"))));
  }
}

