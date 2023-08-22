/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.component.location.Location;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitAdditionalComponentTestCase extends AbstractLazyInitConfigurationComponentLocatorTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    // Initializes some component first to have as a base for the rest of the tests
    lazyComponentInitializer.initializeComponent(builder().globalName("flowFailing").build());

    // Just a couple of control tests regarding the previous setup
    assertFlowsInitialized("flowFailing");
    assertLocationsInitialized("flowFailing", "flowFailing/processors/0");
    assertLocationsNotInitialized("anotherFlow");
  }

  @Test
  public void whenInitializeAdditionalComponentThenPreviousComponentsAreNotDisposed() {
    // Initialize another flow in addition to the previous one
    registry.lookupByName("anotherFlow");
    assertFlowsInitialized("flowFailing", "anotherFlow");
    assertLocationsInitialized("flowFailing", "flowFailing/processors/0", "anotherFlow", "anotherFlow/source");
    assertLocationsNotInitialized("justAnotherFlowThatShouldNotBeInitialized");
  }

  @Test
  public void whenInitializeComponentsAfterInitializeAdditionalThenPreviousComponentsAreDisposed() {
    // Initialize another flow in addition to the previous one
    registry.lookupByName("anotherFlow");
    assertFlowsInitialized("flowFailing", "anotherFlow");
    assertLocationsInitialized("flowFailing", "flowFailing/processors/0", "anotherFlow", "anotherFlow/source");
    assertLocationsNotInitialized("justAnotherFlowThatShouldNotBeInitialized");

    // Initialize *only* another flow
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("flowFailing").build());
    assertFlowsInitialized("flowFailing");
    assertLocationsInitialized("flowFailing");
    assertLocationsNotInitialized("myFlow", "myFlow/source", "anotherFlow", "anotherFlow/source");
  }

  @Description("Initialize same flow with redelivery policy configured in a listener, test component should not fail when initializing the second time")
  @Test
  public void whenInitializingComponentsDependingOnComponentsAlreadyInitializedItDoesNotFail() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("redeliveryPolicyFlowRef1", "redeliveryPolicyFlowRef2", "redeliveryPolicyFlow",
                                  "listenerConfigRedeliveryPolicy");

    invokeInitializer(builder().globalName("redeliveryPolicyFlowRef1").build());
    invokeInitializer(builder().globalName("redeliveryPolicyFlowRef2").build());

    assertLocationsInitialized("redeliveryPolicyFlowRef1", "redeliveryPolicyFlowRef2", "redeliveryPolicyFlow",
                               "listenerConfigRedeliveryPolicy");
  }

  @Override
  protected void invokeInitializer(Location location) {
    registry.lookupByName(location.toString());
  }
}
