/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class FlowRefTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedException = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-ref.xml";
  }

  @Test
  public void twoFlowRefsToSubFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("flow1").withPayload("0").run();
    assertThat(getPayloadAsString(muleEvent.getMessage()), is("012xyzabc312xyzabc3"));
  }

  @Test
  public void dynamicFlowRef() throws Exception {
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "A").run().getMessage().getPayload().getValue(),
               is("0A"));
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "B").run().getMessage().getPayload().getValue(),
               is("0B"));
  }

  @Test
  public void dynamicFlowRefWithChoice() throws Exception {
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "C").run().getMessage().getPayload().getValue(),
               is("0A"));
  }

  @Test
  public void flowRefTargetToFlow() throws Exception {
    assertThat(flowRunner("targetToFlow").run().getVariables().get("flowRefResult").getValue(), is("result"));
  }

  @Test
  public void flowRefTargetToSubFlow() throws Exception {
    assertThat(flowRunner("targetToSubFlow").run().getVariables().get("flowRefResult").getValue(), is("result"));
  }

  @Test
  public void dynamicFlowRefWithScatterGather() throws Exception {
    Map<String, Message> messageList =
        (Map<String, Message>) flowRunner("flow2").withPayload("0").withVariable("letter", "SG").run().getMessage()
            .getPayload().getValue();

    List payloads = messageList.values().stream().map(msg -> msg.getPayload().getValue()).collect(toList());
    assertEquals("0A", payloads.get(0));
    assertEquals("0B", payloads.get(1));
  }

  @Test
  public void flowRefNotFound() throws Exception {
    expectedException.expectMessage(containsString("No flow/sub-flow with name 'sub-flow-Z' found"));
    expectedException.expectErrorType(CORE_NAMESPACE_NAME, ROUTING_ERROR_IDENTIFIER);
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "Z").run().getMessage().getPayload().getValue(),
               is("0C"));
  }

}
