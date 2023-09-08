/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.mule.runtime.api.notification.ExceptionNotification.EXCEPTION_ACTION;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.listener.ExceptionListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.IntegerAction;

import org.junit.Rule;
import org.junit.Test;

public class ExceptionNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/exception-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    ExceptionListener exceptionListener = new ExceptionListener(notificationListenerRegistry);
    expectedError.expectErrorType("APP", "EXPECTED");
    try {
      flowRunner("the-service").withPayload(TEST_PAYLOAD).run().getMessage();
    } finally {
      // processing is async, give time for the exception notificator to run
      exceptionListener.waitUntilAllNotificationsAreReceived();

      assertNotifications();
    }
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node(ExceptionNotification.class, new IntegerAction(EXCEPTION_ACTION));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    verifyAllNotifications(spec, ExceptionNotification.class, EXCEPTION_ACTION, EXCEPTION_ACTION);
  }
}
