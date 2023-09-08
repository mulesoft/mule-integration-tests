/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.core.context.notification.processors.ProcessorNotificationStore;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Abstract test case containing an environment and some tools useful for testing lazy initialization.
 * <p>
 * This abstract test case contains tests that will be executed by all the concrete implementations. That is by design.
 * <p>
 * Each concrete implementation will provide a different implementation of {@link #invokeInitializer(Location)} that will end up
 * affecting the code covered.
 */
@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public abstract class AbstractLazyInitConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  protected static final String MY_SUB_FLOW = "mySubFlow";
  protected static final String FLOW_WITH_SUBFLOW = "flowWithSubflow";

  protected static final String[] EXPECTED_LOCATIONS = {"myFlow",
      "myFlow/source",
      "myFlow/source/0/0",
      "myFlow/processors/0",
      "myFlow/processors/1",
      "myFlow/processors/2",
      "myFlow/processors/2/processors/0",
      "myFlow/processors/2/processors/1",

      "anotherFlow",
      "anotherFlow/source",
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
      "fileListWithMatcherReference/source/0/0",
      "fileListWithMatcherReference/processors/0",

      "listenerConfigRedeliveryPolicy",
      "listenerConfigRedeliveryPolicy/connection",
      "redeliveryPolicyFlow",
      "redeliveryPolicyFlow/source",
      "redeliveryPolicyFlow/source/0",
      "redeliveryPolicyFlow/processors/0",

      "redeliveryPolicyWithObjectStoreFlow",
      "redeliveryPolicyWithObjectStoreFlow/source",
      "redeliveryPolicyWithObjectStoreFlow/source/0",
      "redeliveryPolicyWithObjectStoreFlow/processors/0",

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
      "myObjectStore",

      "notificationFlow",
      "notificationFlow/processors/0",
      "notificationLoggerObject",
      "null",
      "null/0",
      "null",
      "null/0",

      "os-config",
      "os-contains-flow",
      "os-contains-flow/processors/0"};
  protected static final int TOTAL_NUMBER_OF_LOCATIONS = EXPECTED_LOCATIONS.length;

  @Rule
  public final ExpectedException expectedException = none();

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");
  @Rule
  public DynamicPort proxyPort = new DynamicPort("http.proxy.port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  @Inject
  protected Registry registry;

  @Inject
  protected LazyComponentInitializer lazyComponentInitializer;

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
  public boolean disableXmlValidations() {
    return true;
  }

  @Test
  public void whenInitializingComponentsDependingOnErrorTypesThenErrorTypesAreAlsoRegistered() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("redeliveryPolicyFlow", "redeliveryPolicyFlow/source");

    // Initializes a flow with a listener that requires some error types
    invokeInitializer(builderFromStringRepresentation("redeliveryPolicyFlow").build());

    assertLocationsInitialized("redeliveryPolicyFlow", "redeliveryPolicyFlow/source");
  }

  @Test
  public void whenReferencedFromJavaInvokeThenObjectIsInitialized() {
    Location javaInvokeOperationLocation = builder().globalName("invokeBeanFlow").addProcessorsPart().addIndexPart(0).build();

    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("invokeBeanFlow", javaInvokeOperationLocation.toString());

    // Initializes the java:invoke operation
    invokeInitializer(javaInvokeOperationLocation);

    // Checks the bean associated with the method invocation has been instantiated and registered
    assertThat(registry.lookupByName("childBean").isPresent(), is(true));
  }

  @Test
  public void whenParameterDoesNotDefineStereotypeThenReferencedComponentIsAlsoInitialized() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("os-contains-flow", "os-contains-flow/processors/0", "os-config");

    // Initializes the os:contains operation
    invokeInitializer(builderFromStringRepresentation("os-contains-flow").build());

    assertLocationsInitialized("os-contains-flow", "os-contains-flow/processors/0", "os-config");
  }

  @Test
  public void whenComponentHasDeepTransitiveDependenciesThenTheyAreAllInitialized() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("flowLvl0", "flowLvl1", "flowLvl2", "dbConfig", "tlsContextRef", "anonymousProxyConfig");

    // Initializes a flow that has many levels of transitive dependencies with other components
    invokeInitializer(builder().globalName("flowLvl0").build());

    // Checks all direct and transitive dependencies are initialized
    assertLocationsInitialized("flowLvl0", "flowLvl0/processors/0");
    assertLocationsInitialized("flowLvl1", "flowLvl1/processors/0");
    assertLocationsInitialized("flowLvl2", "flowLvl2/processors/0", "flowLvl2/processors/1");
    assertLocationsInitialized("dbConfig", "requestConfig");
    assertLocationsInitialized("tlsContextRef", "anonymousProxyConfig");

    // Control test to see that we didn't just initialize everything
    assertLocationsNotInitialized("listenerConfigRedeliveryPolicy");
  }

  @Test
  public void whenComponentHasRecursiveDependenciesThenTheyAreInitializedWithoutError() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("flowRecursive", "flowRecursive/processors/0");

    invokeInitializer(builder().globalName("flowRecursive").build());

    assertLocationsInitialized("flowRecursive", "flowRecursive/processors/0");
  }

  @Test
  public void whenComponentIsFileListWithMatcherReferenceThenMatcherIsInitialized() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("fileListWithMatcherReference", "Matcher");

    invokeInitializer(builderFromStringRepresentation("fileListWithMatcherReference").build());

    assertLocationsInitialized("fileListWithMatcherReference", "Matcher");
  }

  @Description("Lazy init should create components that are references by other components, even when the reference is not a top level element")
  @Test
  public void whenComponentModelReferencesToNonTopLevelElementThenReferencedElementIsInitialized() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("aggregatorWithMaxSizeFlow/processors/0");

    invokeInitializer(builder().globalName("aggregatorWithMaxSizeListenerFlow").build());

    assertLocationsInitialized("aggregatorWithMaxSizeFlow/processors/0");
    assertLocationsNotInitialized("aggregatorWithMaxSizeFlow", "aggregatorWithMaxSizeFlow/processors/1",
                                  "justAnotherFlowThatShouldNotBeInitialized");
  }

  @Description("Initialize flow and referenced sub-flow")
  @Test
  public void whenInitializingFlowReferencingSubFlowThenSubFlowIsAlsoInitialized() {
    Location mySubFlowLoggerLocation = Location.builder().globalName(MY_SUB_FLOW).addProcessorsPart().addIndexPart(0).build();
    Location flowRefLocation = Location.builder().globalName(FLOW_WITH_SUBFLOW).addProcessorsPart().addIndexPart(0).build();

    // Control test to see these components were not already initialized
    assertLocationsNotInitialized(FLOW_WITH_SUBFLOW, flowRefLocation.toString(), MY_SUB_FLOW, mySubFlowLoggerLocation.toString());

    invokeInitializer(builderFromStringRepresentation(FLOW_WITH_SUBFLOW).build());

    assertLocationsInitialized(FLOW_WITH_SUBFLOW, flowRefLocation.toString(), MY_SUB_FLOW, mySubFlowLoggerLocation.toString());
  }

  @Test
  public void whenNotificationsAreDefinedThenTheyAreAlwaysInitialized() throws Exception {
    invokeInitializer(builder().globalName("notificationFlow").build());

    flowRunner("notificationFlow").run();
    Collection<ProcessorNotificationStore> processorNotificationStores =
        registry.lookupAllByType(ProcessorNotificationStore.class);
    assertThat(processorNotificationStores, hasSize(1));

    processorNotificationStores.stream()
        .forEach(processorNotificationStore -> assertThat(processorNotificationStore.getNotifications(), hasSize(2)));
  }

  @Test
  @Issue("MULE-19928")
  @Description("Initialize an Object Store inside a redelivery policy")
  public void whenFlowHasListenerWithRedeliveryPolicyWithObjectStoreThenObjectStoreIsAlsoInitialized() {
    // Control test to see these components were not already initialized
    assertLocationsNotInitialized("redeliveryPolicyWithObjectStoreFlow", "myObjectStore");

    invokeInitializer(builder().globalName("redeliveryPolicyWithObjectStoreFlow").build());

    assertLocationsInitialized("redeliveryPolicyWithObjectStoreFlow", "myObjectStore");
  }

  protected void assertFlowsInitialized(String... expectedFlowNames) {
    List<String> flowNames = locator.find(buildFromStringRepresentation("flow")).stream()
        .map(c -> ((NamedObject) c).getName())
        .collect(toList());
    assertThat(flowNames, containsInAnyOrder(expectedFlowNames));
  }

  protected void assertLocationsInitialized(String... expectedLocations) {
    for (String location : expectedLocations) {
      assertThat(format("%s is not initialized", location),
                 locator.find(builderFromStringRepresentation(location).build()),
                 is(not(empty())));
    }
  }

  protected void assertLocationsNotInitialized(String... expectedLocations) {
    for (String location : expectedLocations) {
      assertThat(format("%s is initialized", location),
                 locator.find(builderFromStringRepresentation(location).build()),
                 is(empty()));
    }
  }

  protected abstract void invokeInitializer(Location location);
}
