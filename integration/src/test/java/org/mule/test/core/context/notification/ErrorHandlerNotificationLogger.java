/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.ErrorHandlerNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;
import org.mule.runtime.api.notification.ErrorHandlerNotification;

public class ErrorHandlerNotificationLogger extends AbstractNotificationLogger<ErrorHandlerNotification>
    implements ErrorHandlerNotificationListener<ErrorHandlerNotification> {

  @Override
  public boolean isBlocking() {
    return true;
  }
}
