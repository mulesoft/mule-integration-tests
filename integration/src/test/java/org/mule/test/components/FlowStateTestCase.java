/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
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
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName + "Flow");
    // Flow initially started
    assertTrue(flow.getLifecycleState().isStarted());
    assertFalse(flow.getLifecycleState().isStopped());
    assertTrue(((TestMessageSource) flow.getSource()).isStarted());
  }

  @Test
  public void testInitialStateStopped() throws Exception {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("stoppedFlow");
    // Flow initially stopped
    assertFalse(flow.getLifecycleState().isStarted());
    assertTrue(flow.getLifecycleState().isStopped());
    assertFalse(((TestMessageSource) flow.getSource()).isStarted());

    flow.start();
    assertTrue(flow.getLifecycleState().isStarted());
    assertFalse(flow.getLifecycleState().isStopped());
    assertTrue(((TestMessageSource) flow.getSource()).isStarted());
  }

  public static class TestMessageSource extends AbstractAnnotatedObject implements MessageSource, Startable {

    private boolean started;

    @Override
    public void setListener(Processor listener) {

    }

    @Override
    public void start() throws MuleException {
      started = true;
    }

    public boolean isStarted() {
      return started;
    }
  }

}
