/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification.Action;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class ErrorHandlerNotificationTestCase extends AbstractNotificationTestCase {

  private String flowName;

  private Consumer<ExpectedException> expected;
  @Rule
  public ExpectedException expectedException = none();

  @Parameters(name = "{0}")
  public static Object[][] params() {
    return new Object[][] {
        new Object[] {"catch-es", (Consumer<ExpectedException>) (expected -> {
        })},
        new Object[] {"choice-es", (Consumer<ExpectedException>) (expected -> {
        })},
        new Object[] {"rollback-es",
            (Consumer<ExpectedException>) (expected -> expected.expectCause(instanceOf(FunctionalTestException.class)))},
        new Object[] {"default-es",
            (Consumer<ExpectedException>) (expected -> expected.expectCause(instanceOf(FunctionalTestException.class)))}
    };
  }

  public ErrorHandlerNotificationTestCase(String flowName, Consumer<ExpectedException> expected) {
    this.flowName = flowName;
    this.expected = expected;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-strategy-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    expected.accept(expectedException);

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
