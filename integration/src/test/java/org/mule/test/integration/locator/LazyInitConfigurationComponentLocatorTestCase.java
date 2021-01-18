/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.extension.spring.api.SpringConfig;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  private static final int TOTAL_NUMBER_OF_LOCATIONS = 54;
  @Inject
  private Registry registry;

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"org/mule/test/integration/locator/component-locator-config.xml",
        "org/mule/test/integration/locator/component-locator-levels-config.xml",
        "org/mule/test/integration/locator/component-locator-spring-config.xml",
        "org/mule/test/integration/locator/component-locator-reference-component-models.xml"};
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

  @Description("Lazy init should not create components until an operation is done")
  @Test
  public void lazyInitCalculatesLocations() {
    List<String> allLocations = locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList());
    assertThat(allLocations.toString(), allLocations,
               containsInAnyOrder("myFlow",
                                  "myFlow/source",
                                  "myFlow/source/0",
                                  "myFlow/source/0/0",
                                  "myFlow/processors/0",
                                  "myFlow/processors/1",
                                  "myFlow/processors/2",
                                  "myFlow/processors/2/processors/0",
                                  "myFlow/processors/2/processors/1",

                                  "anotherFlow",
                                  "anotherFlow/source",
                                  "anotherFlow/source/0",
                                  "anotherFlow/source/0/0",
                                  "anotherFlow/processors/0",

                                  "flowWithSubflow",
                                  "flowWithSubflow/processors/0",
                                  "mySubFlow",
                                  "mySubFlow/processors/0",

                                  "_muleConfiguration",
                                  "globalErrorHandler",
                                  "globalErrorHandler/0",
                                  "globalErrorHandler/0/processors/0",
                                  "flowFailing",
                                  "flowFailing/processors/0",

                                  "flowLvl0",
                                  "flowLvl0/processors/0",
                                  "flowLvl1",
                                  "flowLvl1/processors/0",
                                  "flowLvl2",
                                  "flowLvl2/processors/0",
                                  "flowLvl2/processors/0/0",
                                  "flowLvl2/processors/1",
                                  "dbConfig",
                                  "dbConfig/0",
                                  "requestConfig",
                                  "requestConfig/0",
                                  "tlsContextRef",
                                  "tlsContextRef/0",
                                  "springConfig",
                                  "globalObjectStore",
                                  "globalObjectStoreAggregatorFlow",
                                  "globalObjectStoreAggregatorFlow/processors/0",
                                  "globalObjectStoreAggregatorFlow/processors/0/route/0",
                                  "globalObjectStoreAggregatorFlow/processors/0/route/0/processors/0",
                                  "aggregatorWithMaxSizeFlow",
                                  "aggregatorWithMaxSizeFlow/processors/0",
                                  "aggregatorWithMaxSizeListenerFlow",
                                  "aggregatorWithMaxSizeListenerFlow/source",
                                  "aggregatorWithMaxSizeListenerFlow/processors/0",
                                  "aggregatorOnListenerFlow",
                                  "aggregatorOnListenerFlow/processors/0",
                                  "aggregatorWithMaxSizeFlow/processors/1",

                                  "justAnotherFlowThatShouldNotBeInitialized",
                                  "justAnotherFlowThatShouldNotBeInitialized/processors/0"));
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
      return location.equals("myFlow/source/0") || location.equals("myFlow/processors/2/processors/0");
    });
    assertThat(muleContext.getConfigurationComponentLocator().findAllLocations(), hasSize(TOTAL_NUMBER_OF_LOCATIONS));

    lazyComponentInitializer.initializeComponents(componentLocation -> {
      String location = componentLocation.getLocation();
      return location.equals("myFlow/source/0") || location.equals("myFlow/processors/2/processors/0");
    });

    assertThat(locator.find(builderFromStringRepresentation("myFlow").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/source").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));
  }

  @Test
  public void lazyMuleContextWithDeeperLevelConfig() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowLvl0").build());

    assertThat(locator.find(builder().globalName("flowLvl0").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl0").addProcessorsPart().addIndexPart(0).build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl1").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl1").addProcessorsPart().addIndexPart(0).build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl2").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(0).build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("dbConfig").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("requestConfig").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("tlsContextRef").build()), is(not(empty())));
  }

  @Test
  public void lazyMuleContextShouldInitializeOnlyTheProcessorRequested() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build());

    assertThat(locator.find(builder().globalName("flowLvl2").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(0).build()), is(empty()));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build()), is(not(empty())));
  }

  @Description("Lazy init should create spring components without dependencies")
  @Test
  public void lazyMuleContextInitializesSpringConfig() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("myFlow"));


    assertThat(locator.find(builderFromStringRepresentation("myFlow").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/source").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/1").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("myFlow/processors/2/processors/1").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));

    assertThat(registry.lookupByName("springConfig").isPresent(), is(true));

    SpringConfig springConfig = (SpringConfig) registry.lookupByName("springConfig").get();
    Object applicationContext = FieldUtils.readField(springConfig, "applicationContext", true);
    assertThat("springConfig was not configured", applicationContext, notNullValue());
  }

  @Description("Spring component should be created each time as the rest")
  @Test
  public void lazyMuleContextSpringConfigRebuilt() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("myFlow"));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));
    Object firstObj = locator.find(builderFromStringRepresentation("springConfig").build()).get();

    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("anotherFlow"));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));
    Object secondObj = locator.find(builderFromStringRepresentation("springConfig").build()).get();
    Object secondAppContext = FieldUtils.readField(secondObj, "applicationContext", true);
    assertThat("springConfig was not configured", secondAppContext, notNullValue());

    assertThat(firstObj, not(sameInstance(secondObj)));
  }

  @Description("Lazy init should create components that are references by other components, when the reference is not a top level element")
  @Test
  public void componentModelReferencesToNonTopLevelElement() {
    lazyComponentInitializer.initializeComponent(builder().globalName("aggregatorWithMaxSizeListenerFlow").build());
    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").addProcessorsPart().addIndexPart(0).build()),
               is(not(empty())));

    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").addProcessorsPart().addIndexPart(1).build()),
               is(empty()));

    assertThat(locator.find(builder().globalName("justAnotherFlowThatShouldNotBeInitialized").build()), is(empty()));
  }

  @Test
  public void globalErrorHandlerApplied() throws Exception {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowFailing").build());

    flowRunner("flowFailing").runExpectingException();

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read("globalErrorHandlerQueue", 5000), is(notNullValue()));
  }
}
