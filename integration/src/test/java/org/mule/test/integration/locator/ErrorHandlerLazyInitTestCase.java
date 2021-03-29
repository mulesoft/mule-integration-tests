/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
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
                                           "APP:ERROR_TYPE_2", "APP:ERROR_TYPE_MAPPING_1");
  }

  @Test
  @Issue("MULE-18286")
  public void errorIsRegisteredButComponentIsNotAnnotated() throws Exception {
    Location location = builder().globalName("errorMappingFlow").build();
    lazyComponentInitializer.initializeComponent(location);

    flowRunner("errorMappingFlow").runExpectingException(errorType("APP", "ERROR_TYPE_MAPPING_1"));
  }

  @Test
  @Issue("MULE-18286")
  public void errorIsRegisteredButComponentIsNotAnnotatedEvenWhenInitializedTwice() throws Exception {
    Location errorMappingFlowLocation = builder().globalName("errorMappingFlow").build();
    lazyComponentInitializer.initializeComponent(errorMappingFlowLocation);
    flowRunner(errorMappingFlowLocation.getGlobalName()).runExpectingException(errorType("APP", "ERROR_TYPE_MAPPING_1"));

    Location errorMappingFlow2Location = builder().globalName("errorMappingFlow2").build();
    lazyComponentInitializer.initializeComponent(errorMappingFlow2Location);
    flowRunner(errorMappingFlow2Location.getGlobalName()).runExpectingException(errorType("APP", "ERROR_TYPE_MAPPING_2"));

    lazyComponentInitializer.initializeComponent(errorMappingFlowLocation);
    flowRunner(errorMappingFlowLocation.getGlobalName()).runExpectingException(errorType("APP", "ERROR_TYPE_MAPPING_1"));
  }

  @Test
  public void emptyRaiseErrorType() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(containsString("type cannot be an empty string or null"));
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("emptyRaiseErrorType").build());
  }

  @Test
  public void invalidErrorTypeOnRaiseError() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(containsString("There's no MULE error named 'ERROR_NON_EXISTING"));
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("invalidErrorTypeOnRaiseError").build());
  }

  @Test
  public void invalidErrorTypeOnErrorHandler() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("invalidErrorTypeOnErrorHandler").build(),
                                           "ERROR_NON_EXISTING_1");
  }

  @Test
  @Issue("MULE-18805")
  @Description("Checks that configuration can be built when having an onError")
  public void withOnErrorReference() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("withSharedContinue").build());
  }

  @Test
  @Issue("MULE-18805")
  @Description("Checks that configuration can be built when having an onError in a global error handler")
  public void globalErrorHandlerWithOnError() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("globalErrorHandlerWithOnError").build());
  }

  @Test
  public void registerCustomErrorsFromErrorMappingOnlyOnce() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("errorMappingFlow").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_2", "APP:ERROR_TYPE_MAPPING_1");
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("errorMappingFlow2").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_2", "APP:ERROR_TYPE_MAPPING_2");
  }

  @Test
  public void registerCustomErrorsFromRaiseError() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("raiseErrorSubFlow").build(), "APP:ERROR_TYPE_1");
  }

  @Test
  public void notEnabledErrorFlow() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("notEnabledFlow").build(), "APP:ERROR_TYPE_1",
                                           "APP:ERROR_TYPE_3");
  }

  @Test
  public void errorShouldNotBeRegisteredFromErrorHandlerNotReferenced() {
    doCustomErrorTypesShouldDiscoveredTest(builder().globalName("notEnabledErrorHandler").build(),
                                           // Before the error repository was determined from the AST, it was populated during the
                                           // initialization of each component.
                                           // Now, this case is consistent regardless of lifecycle or where an error handler is.
                                           // false,
                                           "APP:ERROR_TYPE_4");
  }

  private void doCustomErrorTypesShouldDiscoveredTest(Location location, boolean includeErrorTypes, String... errorTypes) {
    lazyComponentInitializer.initializeComponent(location);

    ErrorTypeRepository errorTypeRepository = registry.lookupByType(ErrorTypeRepository.class)
        .orElseThrow(() -> new AssertionError("Cannot access errorTypeRepository"));

    stream(errorTypes).forEach(errorType -> {
      Optional<ErrorType> appMyErrorType =
          errorTypeRepository.getErrorType(ComponentIdentifier.buildFromStringRepresentation(errorType));

      if (includeErrorTypes) {
        assertThat(errorType, appMyErrorType, not(empty()));
        assertThat(errorType, appMyErrorType.get().getParentErrorType(), equalTo(errorTypeRepository.getAnyErrorType()));
      } else {
        assertThat(errorType, appMyErrorType, equalTo(empty()));
      }
    });
  }

  private void doCustomErrorTypesShouldDiscoveredTest(Location location, String... errorTypes) {
    doCustomErrorTypesShouldDiscoveredTest(location, true, errorTypes);
  }

}
