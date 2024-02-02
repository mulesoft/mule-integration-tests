/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;

@Issue("W-14784223")
public class ErrorHandlerNotificationsTestCase extends AbstractIntegrationTestCase {

  private boolean notificationFired;

  private final ExceptionNotificationListener errorHandlerNotificationListener =
      notification -> this.notificationFired = true;

  @Before
  public void before() {
    notificationListenerRegistry.registerListener(errorHandlerNotificationListener);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler-notifications.xml";
  }

  @Test
  public void doNotFireNotification() throws Exception {
    flowRunner("ErrorPropagateDisableNotifications").runExpectingException();
    assertFalse(notificationFired);
  }

  @Test
  public void fireNotification() throws Exception {
    flowRunner("ErrorPropagateEnableNotifications").runExpectingException();
    assertTrue(notificationFired);
  }
}
