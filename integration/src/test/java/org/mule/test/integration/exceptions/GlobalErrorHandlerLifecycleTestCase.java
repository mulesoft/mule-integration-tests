/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;
import static org.mule.runtime.api.util.MuleSystemProperties.REUSE_GLOBAL_ERROR_HANDLER_PROPERTY;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.TestComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.tck.junit4.rule.SystemProperty;
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
import org.junit.Rule;

@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
public class GlobalErrorHandlerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty reuseGlobalErrorHandler = new SystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, "true");

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
    flowRunner(flow1.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("anotherGlobalErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Startable.PHASE_NAME), is(true));

    ((Lifecycle) flow1).stop();
    ((Lifecycle) flow1).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(false));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(false));

    ((Lifecycle) flow2).stop();
    ((Lifecycle) flow2).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(true));
  }

  @Test
  public void testStopAndStartGlobalErrorHandler() throws Exception {
    flowRunner(globalReference.getName()).run();

    Collection<String> globalErrorHandlerTracker = trackersRegistry.get("globalErrorHandlerTracker").getCalledPhases();

    assertThat(globalErrorHandlerTracker.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(globalErrorHandlerTracker.contains(Startable.PHASE_NAME), is(true));

    ((Lifecycle) globalReference).stop();

    assertThat(globalErrorHandlerTracker.contains(Stoppable.PHASE_NAME), is(true));

    ((Lifecycle) globalReference).start();

    flowRunner(globalReference.getName()).run();

    ((Lifecycle) globalReference).stop();
    ((Lifecycle) globalReference).dispose();

    assertThat(globalErrorHandlerTracker.contains(Disposable.PHASE_NAME), is(true));
  }

}
