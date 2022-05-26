/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.runtime.api.util.MuleSystemProperties.REUSE_GLOBAL_ERROR_HANDLER_PROPERTY;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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

import org.junit.Test;
import org.junit.Rule;

public class DefaultErrorHandlerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-lifecycle.xml";
  }

  @Inject
  private LifecycleTrackerRegistry trackersRegistry;

  @Inject
  @Named("flowD")
  private FlowConstruct flowD;

  @Inject
  @Named("flowE")
  private FlowConstruct flowE;

  @Inject
  @Named("flowF")
  private FlowConstruct flowF;

  @Rule
  public SystemProperty expectedStatus = new SystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, "true");

  @Test
  public void testLifecycleDefaultErrorHandler() throws Exception {
    flowRunner(flowD.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("defaultEhErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases.contains(Initialisable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Startable.PHASE_NAME), is(true));

    ((Lifecycle) flowD).stop();
    ((Lifecycle) flowE).stop();
    ((Lifecycle) flowD).dispose();
    ((Lifecycle) flowE).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(false));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(false));

    ((Lifecycle) flowF).stop();
    ((Lifecycle) flowF).dispose();

    assertThat(defaultEhErrorHandlerPhases.contains(Stoppable.PHASE_NAME), is(true));
    assertThat(defaultEhErrorHandlerPhases.contains(Disposable.PHASE_NAME), is(true));
  }

}
