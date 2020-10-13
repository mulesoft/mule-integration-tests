/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;

import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class PipelineMessageNotificationTestCase extends AbstractNotificationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private String flowName;
  private Class<? extends Throwable> expectedExceptionClz;
  private RestrictedNode spec;
  private Consumer<PipelineMessageNotificationTestCase> assertions;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/pipeline-message-notification-test-flow.xml";
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[][] parameters() {
    return new Object[][] {
        {
            "Request-Response",
            "service-1",
            null,
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (qm) -> {
            }
        },
        {
            "Request-Response Request Exception",
            "service-2",
            ExpressionRuntimeException.class,
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (qm) -> {

            }
        },
        {
            "One-Way",
            "service-4",
            null,
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (t) -> {
              assertNotNull(t.queueManager.read("ow-out", RECEIVE_TIMEOUT, MILLISECONDS));
            }
        },
        {
            "One-Way Request Exception",
            "service-5",
            null,
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (t) -> {
              assertNotNull(t.queueManager.read("owException-out", RECEIVE_TIMEOUT, MILLISECONDS));
            }
        }
    };
  }

  public PipelineMessageNotificationTestCase(String caseName, String flowName, Class<? extends Throwable> expectedExceptionClass,
                                             RestrictedNode spec, Consumer<PipelineMessageNotificationTestCase> assertions) {
    this.flowName = flowName;
    this.expectedExceptionClz = expectedExceptionClass;
    this.spec = spec;
    this.assertions = assertions;
  }

  @Test
  public void doTest() throws Exception {
    try {
      if (expectedExceptionClz != null) {
        expectedException.expectCause(isA(expectedExceptionClz));
      }
      assertNotNull(flowRunner(flowName).withPayload("hi").run());
      assertions.accept(this);
    } finally {
      assertNotifications();
    }
  }

  @Override
  public RestrictedNode getSpecification() {
    return spec;
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    // Nothing to do
  }
}
