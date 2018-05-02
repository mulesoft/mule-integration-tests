/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.tck.core.context.notification.NotificationLogger;

import java.util.LinkedList;
import java.util.List;

public class PipelineAndAsyncMessageNotificationLogger implements NotificationLogger, Disposable {

  protected LinkedList notifications = new LinkedList();

  public List getNotifications() {
    return notifications;
  }

  @Override
  public void dispose() {
    notifications.clear();
  }
}
