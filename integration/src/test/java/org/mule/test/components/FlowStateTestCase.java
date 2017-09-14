/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;

import org.mule.functional.api.component.SkeletonSource;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

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
    LifecycleStateEnabled flow = registry.<LifecycleStateEnabled>lookupByName(flowName + "Flow").get();
    // Flow initially started
    assertTrue(flow.getLifecycleState().isStarted());
    assertFalse(flow.getLifecycleState().isStopped());

    assertTrue(((SkeletonSource) locator.find(builderFromStringRepresentation(flowName + "Flow/source").build()).get())
        .isStarted());
  }

  @Test
  public void testInitialStateStopped() throws Exception {
    LifecycleStateEnabled flow = registry.<LifecycleStateEnabled>lookupByName("stoppedFlow").get();
    // Flow initially stopped
    assertFalse(flow.getLifecycleState().isStarted());
    assertTrue(flow.getLifecycleState().isStopped());

    SkeletonSource source = (SkeletonSource) locator.find(builderFromStringRepresentation("stoppedFlow/source").build()).get();
    assertFalse(source.isStarted());

    registry.<Startable>lookupByName("stoppedFlow").get().start();
    assertTrue(flow.getLifecycleState().isStarted());
    assertFalse(flow.getLifecycleState().isStopped());
    assertTrue(source.isStarted());
  }

}
