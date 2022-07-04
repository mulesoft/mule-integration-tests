/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler.reuseGlobalErrorHandler;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.TestComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import java.util.Collection;

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
  @Named("globalReference")
  private FlowConstruct globalReference;

  @Inject
  @Named("flow1")
  private FlowConstruct flow1;

  @Inject
  @Named("flow2")
  private FlowConstruct flow2;

  private static TestComponentBuildingDefinitionRegistryFactory previous;

  @BeforeClass
  public static void beforeClass() {
    reuseGlobalErrorHandler = true;
    previous = componentBuildingDefinitionRegistryFactory;
    componentBuildingDefinitionRegistryFactory = new TestComponentBuildingDefinitionRegistryFactory();
    componentBuildingDefinitionRegistryFactory.setRefreshRuntimeComponentBuildingDefinitions(true);
  }

  @AfterClass
  public static void afterClass() {
    reuseGlobalErrorHandler = null;
    componentBuildingDefinitionRegistryFactory = previous;
  }

  @Test
  public void testLifecycleGlobalErrorHandler() throws Exception {
    flowRunner(flow1.getName()).run();

    Collection<String> globalErrorHandlerTracker = trackersRegistry.get("anotherGlobalErrorHandlerTracker").getCalledPhases();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) flow1).stop();
    ((Lifecycle) flow1).dispose();

    assertThat(globalErrorHandlerTracker, not(hasItem(Stoppable.PHASE_NAME)));
    assertThat(globalErrorHandlerTracker, not(hasItem(Disposable.PHASE_NAME)));

    ((Lifecycle) flow2).stop();
    ((Lifecycle) flow2).dispose();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Stoppable.PHASE_NAME, Disposable.PHASE_NAME));
  }

  @Test
  public void testStopAndStartGlobalErrorHandler() throws Exception {
    flowRunner(globalReference.getName()).run();

    Collection<String> globalErrorHandlerTracker = trackersRegistry.get("globalErrorHandlerTracker").getCalledPhases();

    assertThat(globalErrorHandlerTracker, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) globalReference).stop();

    assertThat(globalErrorHandlerTracker, hasItem(Stoppable.PHASE_NAME));

    ((Lifecycle) globalReference).start();

    flowRunner(globalReference.getName()).run();

    ((Lifecycle) globalReference).stop();
    ((Lifecycle) globalReference).dispose();

    assertThat(globalErrorHandlerTracker, hasItem(Disposable.PHASE_NAME));
  }

}
