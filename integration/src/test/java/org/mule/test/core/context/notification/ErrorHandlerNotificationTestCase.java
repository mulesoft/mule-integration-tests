/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNotNull;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification.Action;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class ErrorHandlerNotificationTestCase extends AbstractNotificationTestCase {

  private final String flowName;

  private final Consumer<ExpectedError> expected;
  @Rule
  public ExpectedError expectedError = none();

  @Parameters(name = "{0}")
  public static Object[][] params() {
    return new Object[][] {
        new Object[] {"catch-es", (Consumer<ExpectedError>) (expected -> {
        })},
        new Object[] {"choice-es", (Consumer<ExpectedError>) (expected -> {
        })},
        new Object[] {"rollback-es",
            (Consumer<ExpectedError>) (expected -> expected.expectErrorType("APP", "EXPECTED"))},
        new Object[] {"default-es",
            (Consumer<ExpectedError>) (expected -> expected.expectErrorType("APP", "EXPECTED"))}
    };
  }

  public ErrorHandlerNotificationTestCase(String flowName, Consumer<ExpectedError> expected) {
    this.flowName = flowName;
    this.expected = expected;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-strategy-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    expected.accept(expectedError);

    try {
      assertNotNull(flowRunner(flowName).withPayload(TEST_PAYLOAD).run());
    } finally {
      assertNotifications();
    }
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node().parallel(node(new IntegerAction(PROCESS_START))).parallel(node(new IntegerAction(PROCESS_END)));
  }

  private RestrictedNode node(Action action) {
    return new Node(ErrorHandlerNotification.class, action);
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
