/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.mule.runtime.api.util.MuleSystemProperties.HONOUR_RESERVED_PROPERTIES_PROPERTY;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@RunnerDelegateTo(Parameterized.class)
public class ReservedPropertyNamesTestCase extends AbstractIntegrationTestCase {

  public static final String APP_NAME_PROPERTY = "app.name";
  public static final String APP_NAME = "my-app";
  public static final String TEST_PROPERTY_NAME = "key";
  public static final String OTHER_DEPLOYMENT_PROPERTY = "depprop";
  public static final String PROPERTY_VALUE = "custom-properties";
  public static final String CUSTOM_RESOLVER_PROPERTY = "secure::test.key1";
  public static final String CUSTOM_RESOLVER_PROPERTY_VALUE = "test.key1:value1:AES:CBC";

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
  public void propertiesAreResolvedCorrectlyDependingOnVersionAndFeatureFlag() {
    Optional<String> key = configurationProperties.resolveStringProperty(TEST_PROPERTY_NAME);
    assertThat(key.isPresent(), is(true));
    assertThat(key.get(), is(expectedPropertyValue));
  }

  @Test
  public void deploymentPropertiesArePartOfPropertiesResolution() {
    Optional<String> key = configurationProperties.resolveStringProperty(OTHER_DEPLOYMENT_PROPERTY);
    assertThat(key.isPresent(), is(true));
    assertThat(key.get(), is(PROPERTY_VALUE));
  }

  @Test
  public void customPropertiesResolversCanDependOnDeploymentProperties() {
    Optional<String> key = configurationProperties.resolveStringProperty(CUSTOM_RESOLVER_PROPERTY);
    assertThat(key.isPresent(), is(true));
    assertThat(key.get(), is(CUSTOM_RESOLVER_PROPERTY_VALUE));
  }

  @Override
  protected String getConfigFile() {
    return "properties/reserved-property-names-config.xml";
  }

  @Override
  protected DefaultMuleConfiguration createMuleConfiguration() {
    DefaultMuleConfiguration muleConfiguration = super.createMuleConfiguration();
    if (minMuleVersion != null) {
      muleConfiguration.setMinMuleVersion(new MuleVersion(minMuleVersion));
    } else {
      muleConfiguration.setMinMuleVersion(null);
    }
    return muleConfiguration;
  }

  @Override
  protected Map<String, String> artifactProperties() {
    return ImmutableMap.<String, String>builder()
        .put(APP_NAME_PROPERTY, APP_NAME)
        .put(OTHER_DEPLOYMENT_PROPERTY, PROPERTY_VALUE)
        .build();
  }

}
