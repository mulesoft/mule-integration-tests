/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.tck.util.FlowTraceUtils.assertStackElements;
import static org.mule.tck.util.FlowTraceUtils.isFlowStackElement;
import static org.mule.tck.util.FlowTraceUtils.FlowStackAsserter.stackToAssert;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.FLOW_STACK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.tck.util.FlowTraceUtils.FlowStackAsyncAsserter;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LOGGING)
@Story(FLOW_STACK)
public class FlowStackTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

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
  public void xmlSdkOperationNested() throws Exception {
    flowRunner("xmlSdkOperationNested").withPayload("payload").run();

    assertThat(stackToAssert, not(nullValue()));

    assertStackElements(stackToAssert,
                        isFlowStackElement("subFlow",
                                           "subFlow/processors/0"),
                        isFlowStackElement("module-using-core:flow-stack-store",
                                           "flow-stack-store/processors/0"),
                        isFlowStackElement("module-using-core:flow-stack-store-nested",
                                           "flow-stack-store-nested/processors/0"),
                        isFlowStackElement("xmlSdkOperationNested",
                                           "xmlSdkOperationNested/processors/0"));
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

}
