/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.integration.locator.processor.CustomTestComponent;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitPropertyInitTestCase extends AbstractIntegrationTestCase {

  @Inject
  private Registry registry;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/object-with-property.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Test
  @Issue("MULE-18319")
  public void objectWithProperty() {
    CustomTestComponent.statesByInstances.clear();
    final Location testingObjectLocation = builderFromStringRepresentation("testingObject").build();

    lazyComponentInitializer.initializeComponent(testingObjectLocation);

    assertThat(registry.lookupByName("testingObject"), notNullValue());
  }
}
