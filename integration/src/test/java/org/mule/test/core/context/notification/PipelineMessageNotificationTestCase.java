/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.context.notification.AsyncMessageNotification.PROCESS_ASYNC_COMPLETE;
import static org.mule.runtime.core.api.context.notification.AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_START;

import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.api.context.notification.IntegerAction;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.exception.MessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PipelineMessageNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/pipeline-message-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    MuleClient client = muleContext.getClient();
    assertNotNull(flowRunner("service-1").withPayload("hello sweet world").run());
    expectedException.expect(MessagingException.class);
    assertNotNull(flowRunner("service-2").withPayload("hello sweet world").run());
    assertNotNull(flowRunner("service-3").withPayload("hello sweet world").run());
    flowRunner("service-4").withPayload("goodbye cruel world").run();
    client.request("test://ow-out", RECEIVE_TIMEOUT);
    flowRunner("service-5").withPayload("goodbye cruel world").withInboundProperty("fail", "true").run();
    client.request("test://owException-out", RECEIVE_TIMEOUT);

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        // Request-Response
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
        // Request-Response Request Exception
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
        // Request-Response Response Exception
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
        // One-Way
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
        .serial(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_SCHEDULED))
            .parallel(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_SCHEDULED)))
            .parallel(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_COMPLETE)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_COMPLETE))))
        // One-Way Request Exception
        .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
        .serial(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_SCHEDULED))
            .parallel(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_COMPLETE)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_SCHEDULED)))
            .parallel(new Node(AsyncMessageNotification.class, new IntegerAction(PROCESS_ASYNC_COMPLETE))));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    // Nothing to do
  }
}
