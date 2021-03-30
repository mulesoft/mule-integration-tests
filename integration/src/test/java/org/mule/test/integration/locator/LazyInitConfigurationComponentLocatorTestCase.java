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
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.core.context.notification.processors.ProcessorNotificationStore;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  private static final String MY_SUB_FLOW = "mySubFlow";
  private static final String FLOW_WITH_SUBFLOW = "flowWithSubflow";

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");
  @Rule
  public DynamicPort proxyPort = new DynamicPort("http.proxy.port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");


  private static final int TOTAL_NUMBER_OF_LOCATIONS = 97;
  @Inject
  private Registry registry;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "org/mule/test/integration/locator/component-locator-config.xml",
        "org/mule/test/integration/locator/component-locator-notifications.xml",
        "org/mule/test/integration/locator/component-locator-levels-config.xml",
        "org/mule/test/integration/locator/component-locator-os-connector.xml",
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

                                  FLOW_WITH_SUBFLOW,
                                  FLOW_WITH_SUBFLOW + "/processors/0",
                                  MY_SUB_FLOW,
                                  MY_SUB_FLOW + "/processors/0",

                                  OBJECT_MULE_CONFIGURATION,
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
                                  "flowRecursive",
                                  "flowRecursive/processors/0",
                                  "dbConfig",
                                  "dbConfig/connection",
                                  "requestConfig",
                                  "requestConfig/connection",
                                  "tlsContextRef",
                                  "tlsContextRef/0",
                                  "anonymousProxyConfig",

                                  "globalObjectStore",
                                  "globalObjectStoreAggregatorFlow",
                                  "globalObjectStoreAggregatorFlow/processors/0",
                                  "globalObjectStoreAggregatorFlow/processors/0/route/0",
                                  "globalObjectStoreAggregatorFlow/processors/0/route/0/processors/0",
                                  "aggregatorWithMaxSizeFlow",
                                  "aggregatorWithMaxSizeFlow/processors/0",
                                  "aggregatorWithMaxSizeFlow/processors/1",
                                  "aggregatorWithMaxSizeListenerFlow",
                                  "aggregatorWithMaxSizeListenerFlow/source",
                                  "aggregatorWithMaxSizeListenerFlow/processors/0",
                                  "aggregatorOnListenerFlow",
                                  "aggregatorOnListenerFlow/processors/0",

                                  "justAnotherFlowThatShouldNotBeInitialized",
                                  "justAnotherFlowThatShouldNotBeInitialized/processors/0",

                                  "Matcher",
                                  "fileListWithMatcherReference",
                                  "fileListWithMatcherReference/source",
                                  "fileListWithMatcherReference/source/0",
                                  "fileListWithMatcherReference/source/0/0",
                                  "fileListWithMatcherReference/processors/0",

                                  "listenerConfigRedeliveryPolicy",
                                  "listenerConfigRedeliveryPolicy/connection",
                                  "redeliveryPolicyFlow",
                                  "redeliveryPolicyFlow/source",
                                  "redeliveryPolicyFlow/source/0",
                                  "redeliveryPolicyFlow/processors/0",
                                  "redeliveryPolicyFlowRef1",
                                  "redeliveryPolicyFlowRef1/processors/0",
                                  "redeliveryPolicyFlowRef2",
                                  "redeliveryPolicyFlowRef2/processors/0",

                                  "untilSuccessfulFlow",
                                  "untilSuccessfulFlow/processors/0",
                                  "untilSuccessfulFlow/processors/0/processors/0",

                                  "untilSuccessfulFlowCopy",
                                  "untilSuccessfulFlowCopy/processors/0",
                                  "untilSuccessfulFlowCopy/processors/0/processors/0",

                                  "multipleInitialize",
                                  "multipleInitialize/processors/0",
                                  "multipleInitialize/processors/1",

                                  "async-flow",
                                  "async-flow/processors/0",
                                  "async-flow/processors/0/processors/0",

                                  "invokeBeanFlow",
                                  "invokeBeanFlow/processors/0",
                                  "childBean",

                                  "notificationFlow",
                                  "notificationFlow/processors/0",
                                  "notificationLoggerObject",
                                  "null",
                                  "null/0",
                                  "null",
                                  "null/0",

                                  "os-config",
                                  "os-contains-flow",
                                  "os-contains-flow/processors/0"));
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

  @Test
  public void objectInitializedWhenReferenceFromJavaInvoke() {
    Location invokeBeanFlow = builder().globalName("invokeBeanFlow").addProcessorsPart().addIndexPart(0).build();
    lazyComponentInitializer.initializeComponent(invokeBeanFlow);

    assertThat(registry.lookupByName("childBean").isPresent(), is(true));

    assertThat(locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList()), hasItem("childBean"));
  }

  @Test
  public void objectStoreConnectorDoesNotDefineStereotypeOnParameter() {
    Location invokeBeanFlow = builder().globalName("os-contains-flow").addProcessorsPart().addIndexPart(0).build();
    lazyComponentInitializer.initializeComponent(invokeBeanFlow);

    assertThat(locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList()), hasItem("os-config"));
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
    assertThat(locator.find(builder().globalName("anonymousProxyConfig").build()), is(not(empty())));
  }

  @Test
  public void lazyMuleContextWithRecursiveFlowRefs() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowRecursive").build());

    assertThat(locator.find(builder().globalName("flowRecursive").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("flowRecursive").addProcessorsPart().addIndexPart(0).build()), is(not(empty())));
  }

  @Test
  public void fileListShouldInitializeMatcherReference() {
    lazyComponentInitializer
        .initializeComponent(builder().globalName("fileListWithMatcherReference").addProcessorsPart().addIndexPart(0).build());

    assertThat(locator.find(builder().globalName("fileListWithMatcherReference").build()), is(empty()));
    assertThat(locator.find(builder().globalName("fileListWithMatcherReference").addProcessorsPart().addIndexPart(0).build()),
               is(not(empty())));
  }

  @Test
  public void lazyMuleContextShouldInitializeOnlyTheProcessorRequested() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build());

    assertThat(locator.find(builder().globalName("flowLvl2").build()), is(empty()));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(0).build()), is(empty()));
    assertThat(locator.find(builder().globalName("flowLvl2").addProcessorsPart().addIndexPart(1).build()), is(not(empty())));
  }

  @Description("Lazy init should create components that are references by other components, when the reference is not a top level element")
  @Test
  public void componentModelReferencesToNonTopLevelElement() {
    lazyComponentInitializer.initializeComponent(builder().globalName("aggregatorWithMaxSizeListenerFlow").build());
    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").addProcessorsPart().addIndexPart(0).build()),
               is(not(empty())));

    assertThat(locator.find(builder().globalName("aggregatorWithMaxSizeFlow").addProcessorsPart().addIndexPart(1).build()),
               is(empty()));

    assertThat(locator.find(builder().globalName("justAnotherFlowThatShouldNotBeInitialized").build()), is(empty()));
  }

  @Description("Search for sub-flows components")
  @Test
  public void findSubFlowComponents() {
    lazyComponentInitializer.initializeComponent(builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build());

    Optional<Component> componentOptional =
        locator.find(Location.builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build());
    assertThat(componentOptional.isPresent(), is(true));
  }

  @Description("Initialize flow and referenced sub-flow")
  @Test
  public void findFlowAndSubFlowComponents() {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals(FLOW_WITH_SUBFLOW));

    Optional<Component> componentOptional =
        locator.find(Location.builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build());
    assertThat(componentOptional.isPresent(), is(true));

    componentOptional =
        locator.find(Location.builder().globalName(FLOW_WITH_SUBFLOW).addProcessorsPart().addIndexPart(0).build());
    assertThat(componentOptional.isPresent(), is(true));
  }

  @Test
  public void notificationsObjectInitialized() throws Exception {
    lazyComponentInitializer.initializeComponent(builder().globalName("notificationFlow").build());

    flowRunner("notificationFlow").run();
    Collection<ProcessorNotificationStore> processorNotificationStores =
        registry.lookupAllByType(ProcessorNotificationStore.class);
    assertThat(processorNotificationStores, hasSize(1));

    processorNotificationStores.stream()
        .forEach(processorNotificationStore -> assertThat(processorNotificationStore.getNotifications(), hasSize(2)));
  }

  @Description("Initialize same flow with redelivery policy configured in a listener, test component should not fail when initializing the second time")
  @Test
  public void listenerWithRedeliveryPolicyInitializeMultipleTimes() {
    lazyComponentInitializer.initializeComponent(builder().globalName("redeliveryPolicyFlowRef1").build());
    lazyComponentInitializer.initializeComponent(builder().globalName("redeliveryPolicyFlowRef2").build());

    assertThat(locator.find(builder().globalName("redeliveryPolicyFlow").build()), is(not(empty())));
  }

}
