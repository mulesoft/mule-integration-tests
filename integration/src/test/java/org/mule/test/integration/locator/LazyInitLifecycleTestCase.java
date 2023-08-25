/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.locator;

import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.integration.locator.processor.CustomTestComponent;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitLifecycleTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");
  @Rule
  public DynamicPort proxyPort = new DynamicPort("http.proxy.port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  @Inject
  private Registry registry;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-lifecycle-config.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Description("Search for sub-flows with asyncs")
  @Test
  public void subFlowWithAsync() {
    CustomTestComponent.statesByInstances.clear();

    lazyComponentInitializer.initializeComponent(builder().globalName("async-flow").addProcessorsPart().addIndexPart(0).build());

    assertThat(locator.find(builderFromStringRepresentation("async-flow").build()), is(empty()));

    assertThat(CustomTestComponent.statesByInstances.toString(),
               CustomTestComponent.statesByInstances.size(), is(1));
    assertThat(CustomTestComponent.statesByInstances.toString(),
               CustomTestComponent.statesByInstances.values(), containsInAnyOrder("initialized_started"));
  }

  @Description("Initialize same sub-flow twice, test component should not fail when disposing")
  @Test
  public void lazyMuleContextInitializeMultipleTimesSubFlowWithUntilSuccessful() {
    CustomTestComponent.statesByInstances.clear();

    lazyComponentInitializer
        .initializeComponents(componentLocation -> componentLocation.getLocation().equals("untilSuccessfulFlow"));
    lazyComponentInitializer
        .initializeComponents(componentLocation -> componentLocation.getLocation().equals("untilSuccessfulFlowCopy"));

    // force dispose to check that components from sub-flow are disposed
    muleContext.dispose();
    assertThat(CustomTestComponent.statesByInstances.size(), is(2));
    assertThat(CustomTestComponent.statesByInstances.values(),
               containsInAnyOrder("initialized_started_stopped_disposed", "initialized_started_stopped_disposed"));
  }

  @Test
  @Ignore("MULE-18566")
  public void lazyMuleContextInitializeMultipleTimesProcessor() {
    CustomTestComponent.statesByInstances.clear();

    Location multipleInitiailizeProcessor1 =
        builder().globalName("multipleInitialize").addProcessorsPart().addIndexPart(0).build();
    Location multipleInitiailizeProcessor2 =
        builder().globalName("multipleInitialize").addProcessorsPart().addIndexPart(1).build();

    lazyComponentInitializer.initializeComponent(multipleInitiailizeProcessor1);
    assertThat(locator.find(multipleInitiailizeProcessor1), not(empty()));
    assertThat(locator.find(multipleInitiailizeProcessor2), is(empty()));

    MuleConfiguration configuration = registry.lookupByType(MuleConfiguration.class)
        .orElseThrow(() -> new AssertionError("Missing MuleConfiguration from registry"));

    lazyComponentInitializer.initializeComponent(multipleInitiailizeProcessor2);
    assertThat(locator.find(multipleInitiailizeProcessor1), is(empty()));
    assertThat(locator.find(multipleInitiailizeProcessor2), not(empty()));

    MuleConfiguration afterNextInitConfiguration = registry.lookupByType(MuleConfiguration.class)
        .orElseThrow(() -> new AssertionError("Missing MuleConfiguration from registry"));

    // Cannot do more than testing that both are the same instances and equals
    assertThat(configuration, sameInstance(afterNextInitConfiguration));
    assertThat(configuration, equalTo(afterNextInitConfiguration));

    // force dispose to check that components from sub-flow are disposed
    muleContext.dispose();
    assertThat(CustomTestComponent.statesByInstances.size(), is(2));
    assertThat(CustomTestComponent.statesByInstances.values(),
               containsInAnyOrder("initialized_started_stopped_disposed", "initialized_started_stopped_disposed"));
  }

  @Test
  public void shouldNotCreateBeansForSameLocationRequest() {
    CustomTestComponent.statesByInstances.clear();

    Location location = builderFromStringRepresentation("untilSuccessfulFlow").build();
    lazyComponentInitializer.initializeComponent(location);
    lazyComponentInitializer.initializeComponent(location);

    // force dispose to check that components from sub-flow are disposed
    muleContext.dispose();
    assertThat(CustomTestComponent.statesByInstances.size(), is(1));
    assertThat(CustomTestComponent.statesByInstances.values(),
               containsInAnyOrder("initialized_started_stopped_disposed"));
  }

  @Test
  public void shouldNotCreateBeansForSameLocationFilterRequest() {
    CustomTestComponent.statesByInstances.clear();

    LazyComponentInitializer.ComponentLocationFilter componentLocationFilter =
        componentLocation -> componentLocation.getLocation().equals("untilSuccessfulFlow");
    lazyComponentInitializer.initializeComponents(componentLocationFilter);
    lazyComponentInitializer.initializeComponents(componentLocationFilter);

    // force dispose to check that components from sub-flow are disposed
    muleContext.dispose();
    assertThat(CustomTestComponent.statesByInstances.size(), is(1));
    assertThat(CustomTestComponent.statesByInstances.values(),
               containsInAnyOrder("initialized_started_stopped_disposed"));
  }

  @Test
  public void globalMuleConfigurationDefaultResponseTimeout() {
    CustomTestComponent.statesByInstances.clear();

    // A configuration can be retrieved but will have the values set from the DSL, instead default values
    MuleConfiguration configuration = registry.lookupByType(MuleConfiguration.class)
        .orElseThrow(() -> new AssertionError("Missing MuleConfiguration from registry"));
    assertThat(configuration.getDefaultResponseTimeout(), is(10000));
    assertThat(CustomTestComponent.statesByInstances.toString(),
               CustomTestComponent.statesByInstances.size(), is(0));

    // Configuration and its dependent components are initialized at this point...
    lazyComponentInitializer.initializeComponent(builder().globalName("flowFailing").build());
    configuration = registry.lookupByType(MuleConfiguration.class)
        .orElseThrow(() -> new AssertionError("Missing MuleConfiguration from registry"));
    assertThat(configuration.getDefaultResponseTimeout(), is(2001));

    // force dispose to check that components from sub-flow are disposed
    muleContext.dispose();
    assertThat(CustomTestComponent.statesByInstances.toString(),
               CustomTestComponent.statesByInstances.size(), is(1));
    assertThat(CustomTestComponent.statesByInstances.toString(),
               CustomTestComponent.statesByInstances.values(),
               containsInAnyOrder("initialized_started_stopped_disposed"));
  }

  @Test
  public void globalErrorHandlerApplied() throws Exception {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowFailing").build());
    flowRunner("flowFailing").runExpectingException();
    assertThat(queueManager.read("globalErrorHandlerQueue", RECEIVE_TIMEOUT, MILLISECONDS), is(notNullValue()));
  }
}
