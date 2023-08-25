/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.AsyncMessageNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;
import org.mule.runtime.api.notification.AsyncMessageNotification;

public class AsyncMessageNotificationLogger extends AbstractNotificationLogger<AsyncMessageNotification>
    implements AsyncMessageNotificationListener<AsyncMessageNotification> {

  @Override
  public boolean isBlocking() {
    return false;
  }

}
