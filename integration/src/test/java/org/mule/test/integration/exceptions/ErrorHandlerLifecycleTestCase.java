/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
import org.junit.Test;

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

    assertThat(defaultEhErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Startable.PHASE_NAME), is(true));

    ((Lifecycle) flowC).stop();
    ((Lifecycle) flowC).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(true));
  }

  @Test
  public void testLifecycleDefaultErrorHandler() throws Exception {
    flowRunner(flowD.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("defaultEhErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Startable.PHASE_NAME), is(true));

    ((Lifecycle) flowD).stop();
    ((Lifecycle) flowD).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(true));
  }
}
