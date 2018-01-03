/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
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

  @Test
  @Issue("MULE-14285")
  public void flowRefFlowErrorNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = new ArrayList<>();
    setupMessageProcessorNotificationListener(notificationList);

    assertThat(flowRunner("flowRefFlowErrorNotifications").runExpectingException().getCause(),
               instanceOf(IllegalStateException.class));

    assertNotifications(notificationList, "flowRefFlowErrorNotifications/processors/0");
  }

  @Test
  @Issue("MULE-14285")
  public void flowRefSubFlowErrorNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = new ArrayList<>();
    setupMessageProcessorNotificationListener(notificationList);

    assertThat(flowRunner("flowRefSubFlowErrorNotifications").runExpectingException().getCause(),
               instanceOf(IllegalStateException.class));

    assertNotifications(notificationList, "flowRefSubFlowErrorNotifications/processors/0");
  }

  private void setupMessageProcessorNotificationListener(List<MessageProcessorNotification> notificationList) {
    muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class,
                                                            MessageProcessorNotification.class);
    muleContext.getNotificationManager().addListener((MessageProcessorNotificationListener) notification -> {
      notificationList.add((MessageProcessorNotification) notification);
    });
  }

  private void assertNotifications(List<MessageProcessorNotification> notificationList, String name) {
    assertThat(notificationList, hasSize(4));

    MessageProcessorNotification preNotification = notificationList.get(0);
    assertThat(preNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(preNotification.getComponent().getLocation().getLocation(), equalTo(name));
    assertThat(preNotification.getException(), is(nullValue()));

    MessageProcessorNotification postNotification = notificationList.get(3);
    assertThat(postNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(postNotification.getComponent().getLocation().getLocation(), equalTo(name));
    assertThat(postNotification.getException().getCause(), instanceOf(IllegalStateException.class));
    assertThat(postNotification.getEvent().getError().isPresent(), is(true));
    assertThat(postNotification.getEvent().getError().get().getCause(), instanceOf(IllegalStateException.class));
  }

}
