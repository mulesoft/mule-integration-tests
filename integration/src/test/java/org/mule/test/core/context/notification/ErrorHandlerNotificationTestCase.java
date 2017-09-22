/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification.Action;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ErrorHandlerNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();


  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-strategy-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    assertNotNull(flowRunner("catch-es").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("choice-es").withPayload(TEST_PAYLOAD).run());
    expectedException.expectCause(instanceOf(FunctionalTestException.class));
    assertNotNull(flowRunner("rollback-es").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("default-es").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        .serial(node(new IntegerAction(PROCESS_START))
            .serial(node(new IntegerAction(PROCESS_END))))
        .serial(node(new IntegerAction(PROCESS_START))
            .serial(node(new IntegerAction(PROCESS_END))))
        .serial(node(new IntegerAction(PROCESS_START))
            .serial(node(new IntegerAction(PROCESS_END))))
        .serial(node(new IntegerAction(PROCESS_START))
            .serial(node(new IntegerAction(PROCESS_END))));
  }

  private RestrictedNode node(Action action) {
    return new Node(ErrorHandlerNotification.class, action);
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}
}
