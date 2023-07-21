/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.TestComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
public class GlobalErrorHandlerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/global-error-handler-lifecycle.xml";
  }

  @Inject
  private LifecycleTrackerRegistry trackersRegistry;

  @Inject
  @Named("globalFlow1")
  private FlowConstruct globalFlow1;

  @Inject
  @Named("globalFlow2")
  private FlowConstruct globalFlow2;

  @Inject
  @Named("anotherGlobalFlow1")
  private FlowConstruct anotherGlobalFlow1;

  @Inject
  @Named("anotherGlobalFlow2")
  private FlowConstruct anotherGlobalFlow2;

  private static TestComponentBuildingDefinitionRegistryFactory previous;

  @BeforeClass
  public static void beforeClass() {
    previous = componentBuildingDefinitionRegistryFactory;
    componentBuildingDefinitionRegistryFactory = new TestComponentBuildingDefinitionRegistryFactory();
    componentBuildingDefinitionRegistryFactory.setRefreshRuntimeComponentBuildingDefinitions(true);
  }

  @AfterClass
  public static void afterClass() {
    componentBuildingDefinitionRegistryFactory = previous;
  }

  @Test
  public void testLifecycleGlobalErrorHandler() throws Exception {
    flowRunner(anotherGlobalFlow1.getName()).run();

    Collection<String> globalErrorHandlerTracker = trackersRegistry.get("anotherGlobalErrorHandlerTracker").getCalledPhases();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) anotherGlobalFlow1).stop();
    ((Lifecycle) anotherGlobalFlow1).dispose();

    assertThat(globalErrorHandlerTracker, not(hasItem(Stoppable.PHASE_NAME)));
    assertThat(globalErrorHandlerTracker, not(hasItem(Disposable.PHASE_NAME)));

    ((Lifecycle) anotherGlobalFlow2).stop();
    ((Lifecycle) anotherGlobalFlow2).dispose();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Stoppable.PHASE_NAME, Disposable.PHASE_NAME));
  }

  @Test
  public void testStopAndStartGlobalErrorHandler() throws Exception {
    flowRunner(globalFlow1.getName()).run();
    flowRunner(globalFlow2.getName()).run();

    Collection<String> globalErrorHandlerTracker = trackersRegistry.get("globalErrorHandlerTracker").getCalledPhases();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) globalFlow1).stop();

    flowRunner(globalFlow2.getName()).run();

    ((Lifecycle) globalFlow1).start();

    flowRunner(globalFlow1.getName()).run();

    ((Lifecycle) globalFlow1).stop();
    ((Lifecycle) globalFlow2).stop();
    ((Lifecycle) globalFlow1).dispose();
    ((Lifecycle) globalFlow2).dispose();

    assertThat(globalErrorHandlerTracker, hasItem(Disposable.PHASE_NAME));
  }

  @Test
  public void clearChainInStop() throws MuleException {
    FlowExceptionHandler globalErrorHandler = globalFlow1.getMuleContext().getDefaultErrorHandler(empty());
    Map<Component, Consumer<Exception>> routers = globalErrorHandler.getRouters();
    int size = routers.size();

    Optional<Component> forGlobalReference =
        routers.keySet().stream().filter(component -> component.toString().contains(globalFlow1.getName())).findFirst();
    assertThat(forGlobalReference.isPresent(), is(true));
    ((Lifecycle) globalFlow1).stop();

    forGlobalReference =
        routers.keySet().stream().filter(component -> component.toString().contains(globalFlow1.getName())).findFirst();
    assertThat(forGlobalReference.isPresent(), is(false));
    assertThat(routers.size(), is(size - 1));
  }

}
