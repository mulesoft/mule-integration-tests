/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_FLOW_TRACE;
import static org.mule.tck.util.FlowTraceUtils.assertStackElements;
import static org.mule.tck.util.FlowTraceUtils.isFlowStackElement;
import static org.mule.tck.util.FlowTraceUtils.FlowStackAsserter.stackToAssert;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.FLOW_STACK;

import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.FlowTraceUtils.FlowStackAsyncAsserter;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(FLOW_STACK)
public class FlowStackTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty flowTraceEnabled = new SystemProperty(MULE_FLOW_TRACE, "true");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/flow-stack-config.xml";
  }

  @Before
  public void before() {
    muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class,
                                                            MessageProcessorNotification.class);

    stackToAssert = null;
    FlowStackAsyncAsserter.latch = new CountDownLatch(1);
  }

  @Test
  public void flowStatic() throws Exception {
    flowRunner("flowStatic").withPayload(TEST_MESSAGE).run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowStatic", "flowStatic/processors/0"));
  }

  @Test
  public void subFlowStatic() throws Exception {
    flowRunner("subFlowStatic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("subFlowStatic", "subFlowStatic/processors/0"));
  }

  @Test
  public void flowDynamic() throws Exception {
    flowRunner("flowDynamic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowDynamic", "flowDynamic/processors/0"));
  }

  @Test
  public void subFlowDynamic() throws Exception {
    flowRunner("subFlowDynamic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("subFlowDynamic", "subFlowDynamic/processors/0"));
  }

  @Test
  public void secondFlowStatic() throws Exception {
    flowRunner("secondFlowStatic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("secondFlowStatic", "secondFlowStatic/processors/1"));
  }

  @Test
  public void secondSubFlowStatic() throws Exception {
    flowRunner("secondSubFlowStatic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("secondSubFlowStatic", "secondSubFlowStatic/processors/1"));
  }

  @Test
  public void secondFlowDynamic() throws Exception {
    flowRunner("secondFlowDynamic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("secondFlowDynamic", "secondFlowDynamic/processors/1"));
  }

  @Test
  public void secondSubFlowDynamic() throws Exception {
    flowRunner("secondSubFlowDynamic").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("secondSubFlowDynamic", "secondSubFlowDynamic/processors/1"));
  }

  @Test
  public void flowStaticWithAsync() throws Exception {
    flowRunner("flowStaticWithAsync").withPayload("payload").run();

    FlowStackAsyncAsserter.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flowInAsync", "flowInAsync/processors/0"),
                        isFlowStackElement("flowStaticWithAsync", "flowStaticWithAsync/processors/0/processors/0"));
  }

  @Test
  public void subFlowStaticWithAsync() throws Exception {
    flowRunner("subFlowStaticWithAsync").withPayload("payload").run();

    FlowStackAsyncAsserter.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlowInAsync",
                                           "subFlowInAsync/processors/0"),
                        isFlowStackElement("subFlowStaticWithAsync", "subFlowStaticWithAsync/processors/0/processors/0"));
  }

  @Test
  public void flowDynamicWithAsync() throws Exception {
    flowRunner("flowDynamicWithAsync").withPayload("payload").run();

    FlowStackAsyncAsserter.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flowInAsync", "flowInAsync/processors/0"),
                        isFlowStackElement("flowDynamicWithAsync", "flowDynamicWithAsync/processors/0/processors/0"));
  }

  @Test
  public void subFlowDynamicWithAsync() throws Exception {
    flowRunner("subFlowDynamicWithAsync").withPayload("payload").run();

    FlowStackAsyncAsserter.latch.await(RECEIVE_TIMEOUT, MILLISECONDS);

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlowInAsync",
                                           "subFlowInAsync/processors/0"),
                        isFlowStackElement("subFlowDynamicWithAsync", "subFlowDynamicWithAsync/processors/0/processors/0"));
  }

  @Test
  public void flowStaticWithChoice() throws Exception {
    flowRunner("flowStaticWithChoice").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowStaticWithChoice",
                                           "flowStaticWithChoice/processors/0/route/0/processors/0"));
  }

  @Test
  public void subFlowStaticWithChoice() throws Exception {
    flowRunner("subFlowStaticWithChoice").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("subFlowStaticWithChoice",
                                           "subFlowStaticWithChoice/processors/0/route/0/processors/0"));
  }

  @Test
  public void flowDynamicWithChoice() throws Exception {
    flowRunner("flowDynamicWithChoice").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowDynamicWithChoice",
                                           "flowDynamicWithChoice/processors/0/route/0/processors/0"));
  }

  @Test
  public void subFlowDynamicWithChoice() throws Exception {
    flowRunner("subFlowDynamicWithChoice").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("subFlowDynamicWithChoice",
                                           "subFlowDynamicWithChoice/processors/0/route/0/processors/0"));
  }

  @Test
  public void flowStaticWithScatterGather() throws Exception {
    flowRunner("flowStaticWithScatterGather").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowStaticWithScatterGather",
                                           "flowStaticWithScatterGather/processors/0/route/1/processors/0"));
  }

  @Test
  public void subFlowStaticWithScatterGather() throws Exception {
    flowRunner("subFlowStaticWithScatterGather").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow", "subFlow/processors/0"),
                        isFlowStackElement("subFlowStaticWithScatterGather",
                                           "subFlowStaticWithScatterGather/processors/0/route/1/processors/0"));
  }

  @Test
  public void flowDynamicWithScatterGather() throws Exception {
    flowRunner("flowDynamicWithScatterGather").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowDynamicWithScatterGather",
                                           "flowDynamicWithScatterGather/processors/0/route/1/processors/0"));
  }

  @Test
  public void subFlowDynamicWithScatterGather() throws Exception {
    flowRunner("subFlowDynamicWithScatterGather").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("subFlowDynamicWithScatterGather",
                                           "subFlowDynamicWithScatterGather/processors/0/route/1/processors/0"));
  }

  @Test
  public void flowStaticWithScatterGatherChain() throws Exception {
    flowRunner("flowStaticWithScatterGatherChain").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowStaticWithScatterGatherChain",
                                           "flowStaticWithScatterGatherChain/processors/0/route/1/processors/0"));
  }

  @Test
  public void subFlowStaticWithScatterGatherChain() throws Exception {
    flowRunner("subFlowStaticWithScatterGatherChain").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("subFlowStaticWithScatterGatherChain",
                                           "subFlowStaticWithScatterGatherChain/processors/0/route/1/processors/0"));
  }

  @Test
  public void flowDynamicWithScatterGatherChain() throws Exception {
    flowRunner("flowDynamicWithScatterGatherChain").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert, isFlowStackElement("flow", "flow/processors/0"),
                        isFlowStackElement("flowDynamicWithScatterGatherChain",
                                           "flowDynamicWithScatterGatherChain/processors/0/route/1/processors/0"));
  }

  @Test
  public void subFlowDynamicWithScatterGatherChain() throws Exception {
    flowRunner("subFlowDynamicWithScatterGatherChain").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("subFlowDynamicWithScatterGatherChain",
                                           "subFlowDynamicWithScatterGatherChain/processors/0/route/1/processors/0"));
  }

  @Test
  public void flowForEach() throws Exception {
    flowRunner("flowForEach").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("flow",
                                           "flow/processors/0"),
                        isFlowStackElement("flowForEach",
                                           "flowForEach/processors/0/processors/1"));
  }

  @Test
  public void xmlSdkOperation() throws Exception {
    flowRunner("xmlSdkOperation").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("module-using-core:flow-stack-store",
                                           "flow-stack-store/processors/0"),
                        isFlowStackElement("xmlSdkOperation",
                                           "xmlSdkOperation/processors/0"));
  }

  @Test
  public void xmlSdkOperationInSubflow() throws Exception {
    flowRunner("xmlSdkOperationInSubflow").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("module-using-core:flow-stack-store",
                                           "flow-stack-store/processors/0"),
                        isFlowStackElement("xmlSdkOperation",
                                           "xmlSdkOperation/processors/0"),
                        isFlowStackElement("xmlSdkOperationInSubflow",
                                           "xmlSdkOperationInSubflow/processors/0"));
  }

  @Test
  public void xmlSdkOperationFailAfter() throws Exception {
    flowRunner("xmlSdkOperationFailAfter").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("xmlSdkOperationFailAfter",
                                           "xmlSdkOperationFailAfter/errorHandler/0/processors/0"));
  }

  @Test
  public void xmlSdkOperationFailAfterSubFlow() throws Exception {
    flowRunner("xmlSdkOperationFailAfterSubFlow").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("xmlSdkOperationFailAfter",
                                           "xmlSdkOperationFailAfter/errorHandler/0/processors/0"),
                        isFlowStackElement("xmlSdkOperationFailAfterSubFlow",
                                           "xmlSdkOperationFailAfterSubFlow/processors/0"));
  }

  @Test
  public void xmlSdkOperationAfter() throws Exception {
    flowRunner("xmlSdkOperationAfter").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("xmlSdkOperationAfter",
                                           "xmlSdkOperationAfter/processors/1"));
  }

  @Test
  public void xmlSdkOperationError() throws Exception {
    flowRunner("xmlSdkOperationError").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("xmlSdkOperationError",
                                           "xmlSdkOperationError/processors/1"));
  }

  @Test
  public void xmlSdkOperationErrorInSubflow() throws Exception {
    flowRunner("xmlSdkOperationErrorInSubflow").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("xmlSdkOperationError",
                                           "xmlSdkOperationError/processors/1"),
                        isFlowStackElement("xmlSdkOperationErrorInSubflow",
                                           "xmlSdkOperationErrorInSubflow/processors/0"));
  }

  @Test
  public void flowParallelForeach() throws Exception {
    flowRunner("flowParallelForeach").run();
    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("flow",
                                           "flow/processors/0"),
                        isFlowStackElement("flowParallelForeach",
                                           "flowParallelForeach/processors/0/processors/1"));
  }
}
