/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationComponentLocatorTestCase extends AbstractLazyInitConfigurationComponentLocatorTestCase {

  @Test
  public void whenInitializeComponentAndComponentDoesNotExistThenFails() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("No object found at location non-existent");
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("non-existent").build());
  }

  @Description("Lazy init should not create components until an operation is done")
  @Test
  public void lazyInitCalculatesLocations() {
    List<String> allLocations = locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList());
    assertThat(allLocations.toString(), allLocations,
               containsInAnyOrder(EXPECTED_LOCATIONS));
    assertThat(locator.find(builder().globalName("myFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("anotherFlow").build()), is(empty()));
  }

  @Description("Lazy init should create components when operation is done")
  @Test
  public void lazyMuleContextInitializesLocation() {
    lazyComponentInitializer.initializeComponent(builder().globalName("myFlow").build());
    assertThat(locator.find(builder().globalName("myFlow").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("anotherFlow").build()), is(empty()));

    assertThat(locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList()), hasItem("myFlow/source"));
  }

  @Description("Lazy init should refresh the ConfigurationComponentLocator when initialize is done")
  @Test
  public void lazyMuleContextRefreshesConfigurationComponentLoader() {
    lazyComponentInitializer.initializeComponent(builder().globalName("myFlow").build());
    assertThat(locator.findAllLocations(), hasSize(TOTAL_NUMBER_OF_LOCATIONS));

    lazyComponentInitializer.initializeComponent(builder().globalName("anotherFlow").build());
    assertThat(locator.findAllLocations(), hasSize(TOTAL_NUMBER_OF_LOCATIONS));

    assertThat(locator.find(builder().globalName("myFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("anotherFlow").build()),
               is(not(empty())));
  }

  @Description("Lazy init should keep siblings enabled when requested")
  @Test
  public void lazyMuleContextSiblingNodesEnabled() {
    lazyComponentInitializer.initializeComponents(componentLocation -> {
      String location = componentLocation.getLocation();
      return location.equals("myFlow/source") || location.equals("myFlow/processors/2/processors/0");
    });
    assertThat(locator.findAllLocations(), hasSize(TOTAL_NUMBER_OF_LOCATIONS));

    lazyComponentInitializer.initializeComponents(componentLocation -> {
      String location = componentLocation.getLocation();
      return location.equals("myFlow/source") || location.equals("myFlow/processors/2/processors/0");
    });

    assertThat(locator.find(builderFromStringRepresentation("myFlow").build()), is(empty()));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/source").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2").build()), is(empty()));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2/processors/0").build()), is(not(empty())));
  }

  @Test
  public void lazyMuleContextShouldInitializeOnlyTheProcessorRequested() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build());

    assertThat(locator.find(builder().globalName("flowLvl2").build()), is(empty()));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(0).build()), is(empty()));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build()), is(not(empty())));
  }

  @Description("Search for sub-flows components")
  @Test
  public void findSubFlowComponents() {
    lazyComponentInitializer.initializeComponent(builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build());

    Optional<Component> componentOptional =
        locator.find(Location.builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build());
    assertThat(componentOptional.isPresent(), is(true));
  }

  @Description("Initialize same flow with redelivery policy configured in a listener, test component should not fail when initializing the second time")
  @Test
  public void whenInitializingComponentsDependingOnComponentsAlreadyInitializedItDoesNotFail() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("redeliveryPolicyFlowRef1", "redeliveryPolicyFlowRef2", "redeliveryPolicyFlow",
                                  "listenerConfigRedeliveryPolicy");

    invokeInitializer(builder().globalName("redeliveryPolicyFlowRef1").build());
    invokeInitializer(builder().globalName("redeliveryPolicyFlowRef2").build());

    assertLocationsInitialized("redeliveryPolicyFlowRef2", "redeliveryPolicyFlow", "listenerConfigRedeliveryPolicy");
  }

  @Override
  protected void invokeInitializer(Location location) {
    lazyComponentInitializer.initializeComponent(location);
  }

}
