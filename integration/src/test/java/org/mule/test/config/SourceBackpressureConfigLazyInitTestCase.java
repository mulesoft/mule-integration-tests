/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

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
  protected ConfigurationBuilder getBuilder() throws Exception {
    final ConfigurationBuilder configurationBuilder = createConfigurationBuilder(getConfigFile(), true);
    configureSpringXmlConfigurationBuilder(configurationBuilder);
    return configurationBuilder;
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
