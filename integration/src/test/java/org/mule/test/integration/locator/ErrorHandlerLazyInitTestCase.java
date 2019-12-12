/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class ErrorHandlerLazyInitTestCase extends AbstractIntegrationTestCase {

  @Inject
  private Registry registry;

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Rule
  public ExpectedException expectedException = none();

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
  public void registerCustomErrorsFromErrorHandler() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("mainFlow").build(), "APP:ERROR_TYPE_1", "APP:ERROR_TYPE_2");
  }

  @Test
  public void registerCustomErrorsFromErrorMapping() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("errorMappingFlow").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_2");
  }

  @Test
  public void emptyRaiseErrorType() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    expectedException.expectMessage("type cannot be an empty string or null");
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("emptyRaiseErrorType").build());
  }

  @Test
  public void invalidErrorTypeOnRaiseError() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectCause(instanceOf(InitialisationException.class));
    expectedException.expectMessage("Could not find error 'ERROR_NON_EXISTING'");
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("invalidErrorTypeOnRaiseError").build());
  }

  @Test
  public void invalidErrorTypeOnErrorHandler() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("invalidErrorTypeOnErrorHandler").build(),
                                           "ERROR_NON_EXISTING_1");
  }

  @Test
  public void registerCustomErrorsFromErrorMappingOnlyOnce() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("errorMappingFlow").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_2");
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("errorMappingFlow2").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_2", "APP:ERROR_TYPE_5");
  }

  @Test
  public void registerCustomErrorsFromRaiseError() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("raiseErrorSubFlow").build(), "APP:ERROR_TYPE_1");
  }

  @Test
  public void notEnabledErrorFlow() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("raiseErrorSubFlow").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_3");
  }

  @Test
  public void errorShouldNotBeRegisteredFromErrorHandlerNotReferenced() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("notEnabledErrorHandler").build(), false, "APP:ERROR_TYPE_4");
  }

  private void doCustomErrorTypesShouldDiscoveredTest(Location location, boolean includeErrorTypes, String... errorTypes) {
    lazyComponentInitializer.initializeComponent(location);

    ErrorTypeRepository errorTypeRepository = registry.lookupByType(ErrorTypeRepository.class)
        .orElseThrow(() -> new AssertionError("Cannot access errorTypeRepository"));

    stream(errorTypes).forEach(errorType -> {
      Optional<ErrorType> appMyErrorType =
          errorTypeRepository.getErrorType(ComponentIdentifier.buildFromStringRepresentation(errorType));

      if (includeErrorTypes) {
        assertThat(appMyErrorType, not(empty()));
        assertThat(appMyErrorType.get().getParentErrorType(), equalTo(errorTypeRepository.getAnyErrorType()));
      } else {
        assertThat(appMyErrorType, equalTo(empty()));
      }
    });
  }

  private void doCustomErrorTypesShouldDiscoveredTest(Location location, String... errorTypes) {
    doCustomErrorTypesShouldDiscoveredTest(location, true, errorTypes);
  }

}
