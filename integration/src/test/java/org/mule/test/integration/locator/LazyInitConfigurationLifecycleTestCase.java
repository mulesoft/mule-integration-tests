/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.functional.api.component.LifecycleTrackerConfig;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationLifecycleTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "org/mule/test/integration/locator/component-lifecycle-config.xml"};
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    final ConfigurationBuilder configurationBuilder = createConfigurationBuilder(getConfigFiles(), true);
    configureSpringXmlConfigurationBuilder(configurationBuilder);
    return configurationBuilder;
  }

  @Test
  @Issue("MULE-18417")
  public void nestedConfigLifecycle() {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("rootConfig"));

    final Optional<LifecycleTrackerConfig> nestedConfig = locator.find(builderFromStringRepresentation("rootConfig/0").build())
        .map(c -> (LifecycleTrackerConfig) c);

    assertThat(nestedConfig.isPresent(), is(true));
    assertThat(nestedConfig.get().getTracker(), is(asList("setMuleContext")));
  }

}
