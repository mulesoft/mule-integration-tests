/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.DEFAULT_ERROR_HANDLER;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;

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
@Story(DEFAULT_ERROR_HANDLER)
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

  @Test
  public void testLifecycleDefaultErrorHandler() throws Exception {
    flowRunner(flowD.getName()).run();

    Collection<String> defaultEhErrorHandlerPhases = trackersRegistry.get("defaultEhErrorHandlerTracker").getCalledPhases();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Initialisable.PHASE_NAME, Startable.PHASE_NAME));

    ((Lifecycle) flowD).stop();
    ((Lifecycle) flowE).stop();
    ((Lifecycle) flowD).dispose();
    ((Lifecycle) flowE).dispose();

    assertThat(defaultEhErrorHandlerPhases, not(hasItem(Stoppable.PHASE_NAME)));
    assertThat(defaultEhErrorHandlerPhases, not(hasItem(Disposable.PHASE_NAME)));

    ((Lifecycle) flowF).stop();
    ((Lifecycle) flowF).dispose();

    assertThat(defaultEhErrorHandlerPhases, containsInRelativeOrder(Stoppable.PHASE_NAME, Disposable.PHASE_NAME));
  }
}
