/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Feature(LAZY_INITIALIZATION)
@Story(BACKPRESSURE)
public class SourceBackpressureConfigLazyInitTestCase extends AbstractIntegrationTestCase {

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/source-backpressure-config.xml";
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
  @Issue("MULE-19117")
  public void defaultToFail() {
    Location location = builder().globalName("defaultToFail").build();
    lazyComponentInitializer.initializeComponent(location);
  }

  @Test
  public void configuredToDrop() {
    Location location = builder().globalName("configuredToDrop").build();
    lazyComponentInitializer.initializeComponent(location);
  }

}
