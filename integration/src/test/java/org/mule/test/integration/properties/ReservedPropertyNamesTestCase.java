/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.HONOUR_RESERVED_PROPERTIES_PROPERTY;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.mule.test.runner.RunnerDelegateTo;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@RunnerDelegateTo(Parameterized.class)
public class ReservedPropertyNamesTestCase extends AbstractIntegrationTestCase {

  public static final String APP_NAME_PROPERTY = "app.name";
  public static final String APP_NAME = "my-app";
  public static final String TEST_PROPERTY_NAME = "key";

  @Inject
  private ConfigurationProperties configurationProperties;

  @Rule
  public SystemProperty systemProperty;

  private final String minMuleVersion;

  private final String expectedPropertyValue;

  @Parameters(name = "With minMuleVersion {0} and override value \"{1}\"")
  public static Object[][] parameters() {
    return new Object[][] {
        new Object[] {"4.1.5", "true", "value"},
        new Object[] {"4.2.2", "true", "value"},
        new Object[] {"4.3.0", "true", "value"},

        new Object[] {"4.1.5", null, "overridden.value"},
        new Object[] {"4.2.2", null, "overridden.value"},
        new Object[] {"4.3.0", null, "value"},

        new Object[] {"4.1.5", "false", "overridden.value"},
        new Object[] {"4.2.2", "false", "overridden.value"},
        new Object[] {"4.3.0", "false", "overridden.value"}
    };
  }

  public ReservedPropertyNamesTestCase(String minMuleVersion, String systemPropertyValue, String expectedPropertyValue) {
    this.minMuleVersion = minMuleVersion;
    this.expectedPropertyValue = expectedPropertyValue;
    if (systemPropertyValue != null) {
      systemProperty = new SystemProperty(HONOUR_RESERVED_PROPERTIES_PROPERTY, systemPropertyValue);
    }
  }

  @Test
  public void propertiesAreResolvedCorrectly() {
    Optional<String> key = configurationProperties.resolveStringProperty(TEST_PROPERTY_NAME);

    assertThat(key.isPresent(), is(true));
    assertThat(key.get(), is(expectedPropertyValue));
  }

  @Override
  protected String getConfigFile() {
    return "properties/reserved-property-names-config.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {

      }

      @Override
      public void configure(MuleContext muleContext) {
        if (minMuleVersion != null) {
          ((DefaultMuleConfiguration) muleContext.getConfiguration()).setMinMuleVersion(new MuleVersion(minMuleVersion));
        }
      }
    });
    super.addBuilders(builders);
  }

  @Override
  protected Map<String, String> artifactProperties() {
    return ImmutableMap.<String, String>builder()
        .put(APP_NAME_PROPERTY, APP_NAME)
        .build();
  }

}
