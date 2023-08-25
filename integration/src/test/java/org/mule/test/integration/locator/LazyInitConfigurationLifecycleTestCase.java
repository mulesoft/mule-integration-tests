/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.locator;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.functional.api.component.LifecycleTrackerConfig;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationLifecycleTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-lifecycle-config.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Test
  @Issue("MULE-18417")
  @Ignore("MULE-18566")
  public void nestedConfigLifecycle() {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("rootConfig"));

    final Optional<LifecycleTrackerConfig> nestedConfig = locator.find(builderFromStringRepresentation("rootConfig/0").build())
        .map(c -> (LifecycleTrackerConfig) c);

    assertThat(nestedConfig.isPresent(), is(true));
    assertThat(nestedConfig.get().getTracker(), is(asList("setMuleContext")));
  }

}
