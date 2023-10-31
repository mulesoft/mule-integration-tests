/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseStory.LIFECYCLE_PHASE_STORY;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_STORY)
public class FlowStateTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty initialState = new SystemProperty("state", "started");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/flow-initial-state.xml";
  }

  @Test
  public void testDefaultInitialState() throws Exception {
    doTestStarted("default");
  }

  @Test
  public void testStartedInitialState() throws Exception {
    doTestStarted("started");
  }

  @Test
  public void testPlaceholderStartedInitialState() throws Exception {
    doTestStarted("placeholder");
  }

  protected void doTestStarted(String flowName) throws Exception {
    Flow flow = registry.<Flow>lookupByName(flowName + "Flow").get();
    // Flow initially started
    assertStarted(flow);

    assertTrue(((LifecycleStateEnabled) locator.find(builderFromStringRepresentation(flowName + "Flow/source").build()).get())
        .getLifecycleState().isStarted());
  }

  @Test
  public void testInitialStateStopped() throws Exception {
    Flow flow = registry.<Flow>lookupByName("stoppedFlow").get();
    // Flow initially stopped
    assertStopped(flow);

    LifecycleStateEnabled source =
        (LifecycleStateEnabled) locator.find(builderFromStringRepresentation("stoppedFlow/source").build()).get();
    assertFalse(source.getLifecycleState().isStarted());

    registry.<Startable>lookupByName("stoppedFlow").get().start();
    assertStarted(flow);
    assertTrue(source.getLifecycleState().isStarted());
  }

  @Test
  @Issue("MULE-18059")
  @Description("Make sure that the `initialState` flag in a flow is honored when restarting the app.")
  public void appResetKeepsFlowInitialState() throws Exception {
    Flow flow = registry.<Flow>lookupByName("stoppedFlow").get();
    // Flow initially stopped
    assertStopped(flow);

    muleContext.stop();
    muleContext.start();

    // Flow initially stopped
    assertStopped(flow);
  }

  private void assertStarted(Flow flow) {
    assertTrue(flow.getLifecycleState().isStarted());
    assertFalse(flow.getLifecycleState().isStopped());

    // assert consistency between the Flow and LifecycleStateEnabled interfaces
    assertTrue(flow.isStarted());
    assertFalse(flow.isStopped());
  }

  private void assertStopped(Flow flow) {
    assertFalse(flow.getLifecycleState().isStarted());
    assertTrue(flow.getLifecycleState().isStopped());

    // assert consistency between the Flow and LifecycleStateEnabled interfaces
    assertFalse(flow.isStarted());
    assertTrue(flow.isStopped());
  }
}
