/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Optional.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class ErrorHandlerLazyInitTestCase extends AbstractIntegrationTestCase {

  @Inject
  private Registry registry;

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"org/mule/test/integration/locator/component-locator-error-mapping.xml"};
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
  public void customErrorTypesShouldDiscovered() {
    lazyComponentInitializer.initializeComponent(builder().globalName("mainFlow").build());

    ErrorTypeRepository errorTypeRepository = registry.lookupByType(ErrorTypeRepository.class)
        .orElseThrow(() -> new AssertionError("Cannot access errorTypeRepository"));

    assertApplicationCustomErrorTypes(errorTypeRepository, "APP:ERROR_TYPE_1");
    assertApplicationCustomErrorTypes(errorTypeRepository, "APP:ERROR_TYPE_2");
  }

  public void assertApplicationCustomErrorTypes(ErrorTypeRepository errorTypeRepository, String errorType) {
    Optional<ErrorType> appMyErrorType =
        errorTypeRepository.getErrorType(ComponentIdentifier.buildFromStringRepresentation(errorType));
    assertThat(appMyErrorType, not(empty()));
    assertThat(appMyErrorType.get().getParentErrorType(), equalTo(errorTypeRepository.getAnyErrorType()));
  }

}
