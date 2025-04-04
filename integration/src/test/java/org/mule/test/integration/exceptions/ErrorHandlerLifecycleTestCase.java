/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import java.util.Collection;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class ErrorHandlerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-lifecycle.xml";
  }

  @Inject
  private LifecycleTrackerRegistry trackersRegistry;

  @Inject
  @Named("flowA")
  private FlowConstruct flowA;

  @Inject
  @Named("flowB")
  private FlowConstruct flowB;

  @Inject
  @Named("flowC")
  private FlowConstruct flowC;

  @Inject
  @Named("flowD")
  private FlowConstruct flowD;

  @Inject
  @Named("flowE")
  private FlowConstruct flowE;

  @Inject
  @Named("flowF")
  private FlowConstruct flowF;

  @Test
  public void testLifecycleErrorHandlerInFlow() throws Exception {
    // Trigger the flows so the lifecycle-trackers are added to the registry
    flowRunner(flowA.getName()).run();
    flowRunner(flowB.getName()).run();

    Collection<String> flowAErrorHandlerPhases = trackersRegistry.get("flowAErrorHandlerTracker").getCalledPhases();
    Collection<String> flowBErrorHandlerPhases = trackersRegistry.get("flowBErrorHandlerTracker").getCalledPhases();

    assertThat(flowAErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(flowBErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));

    ((Lifecycle) flowA).stop();

    assertThat(flowAErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(true));
    assertThat(flowBErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(false));
  }

  @Test
  public void testLifecycleReferencedErrorHandler() throws Exception {
    flowRunner(flowC.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("esAErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) flowC).stop();
    ((Lifecycle) flowC).dispose();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Stoppable.PHASE_NAME, Disposable.PHASE_NAME));
  }

  @Test
  public void testLifecycleDefaultErrorHandler() throws Exception {
    flowRunner(flowD.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("defaultEhErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) flowD).stop();
    ((Lifecycle) flowD).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(false));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(false));

    ((Lifecycle) flowE).stop();
    ((Lifecycle) flowE).dispose();

    ((Lifecycle) flowF).stop();
    ((Lifecycle) flowF).dispose();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Stoppable.PHASE_NAME, Disposable.PHASE_NAME));
  }
}
