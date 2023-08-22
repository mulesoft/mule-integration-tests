/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class PipelineMessageNotificationTestCase extends AbstractNotificationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  private String flowName;
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
            DefaultMuleException.class,
            asList("APP", "ERROR"),
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
            null,
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_END)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (t) -> {
              assertNotNull(t.queueManager.read("owException-out", RECEIVE_TIMEOUT, MILLISECONDS));
            }
        },
        {
            "One-Way Nested flow-ref Request Exception",
            "nestedFlowFailingRoot",
            DefaultMuleException.class,
            asList("APP", "ERROR"),
            new Node()
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_START)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE)))
                .serial(new Node(PipelineMessageNotification.class, new IntegerAction(PROCESS_COMPLETE))),
            (Consumer<PipelineMessageNotificationTestCase>) (t) -> {
            }
        }
    };
  }

  public PipelineMessageNotificationTestCase(String caseName, String flowName, Class<? extends Throwable> expectedExceptionClass,
                                             List<String> expectedErrorType,
                                             RestrictedNode spec, Consumer<PipelineMessageNotificationTestCase> assertions) {
    this.flowName = flowName;
    if (expectedExceptionClass != null) {
      expectedError.expectCause(isA(expectedExceptionClass));
      if (expectedErrorType != null) {
        assertThat(expectedErrorType.size(), is(2));
        expectedError.expectErrorType(expectedErrorType.get(0), expectedErrorType.get(1));
      }
    }
    this.spec = spec;
    this.assertions = assertions;
  }

  @Test
  public void doTest() throws Exception {
    try {
      flowRunner(flowName).withPayload("hi").run();
    } finally {
      assertions.accept(this);
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
