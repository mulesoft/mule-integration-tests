/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import javax.inject.Inject;

import org.junit.Test;

public class SubFlowTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LifecycleTrackerRegistry trackersRegistry;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/sub-flow.xml";
  }

  @Test
  public void testSubFlowViaProcessorRef() throws Exception {
    CoreEvent result = flowRunner("SubFlowViaProcessorRef").withPayload("").run();
    assertThat(result.getMessage().getPayload().getValue(), is("1xyz2"));

    assertThat(trackersRegistry.get("subFlowTracker").getCalledPhases(), contains("setMuleContext", "initialise", "start"));
  }

  @Test
  public void testSubFlowViaFlowRef() throws Exception {
    CoreEvent result = flowRunner("SubFlowViaFlowRef").withPayload("").run();
    assertThat(result.getMessage().getPayload().getValue(), is("1xyz2"));

    assertThat(trackersRegistry.get("subFlowTracker").getCalledPhases(), contains("setMuleContext", "initialise", "start"));
  }

  @Test
  public void testFlowviaFlowRef() throws Exception {
    assertThat(getPayloadAsString(flowRunner("FlowViaFlowRef").withPayload("").run().getMessage()), is("1xyz2"));
  }

  @Test
  public void testServiceviaFlowRef() throws Exception {
    assertThat(getPayloadAsString(flowRunner("ServiceViaFlowRef").withPayload("").run().getMessage()), is("1xyz2"));
  }

  @Test
  public void testFlowWithSubFlowWithComponent() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithsubFlowWithComponent").withPayload("0").run().getMessage()), is("0"));

  }

  @Test
  public void testFlowWithSameSubFlowTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameSubFlowTwice").withPayload("0").run().getMessage()), is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameSubFlowSingletonTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameSubFlowSingletonTwice").withPayload("0").run().getMessage()),
               is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameGlobalChainTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameGlobalChainTwice").withPayload("0").run().getMessage()), is("0xyzxyz"));
  }

  @Test
  public void testFlowWithSameGlobalChainSingletonTwice() throws Exception {
    assertThat(getPayloadAsString(flowRunner("flowWithSameGlobalChainSingletonTwice").withPayload("0").run().getMessage()),
               is("0xyzxyz"));
  }

}
