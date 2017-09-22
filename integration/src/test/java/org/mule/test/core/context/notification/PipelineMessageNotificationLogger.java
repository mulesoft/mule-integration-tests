/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.PipelineMessageNotification;

import java.util.List;

public class PipelineMessageNotificationLogger extends PipelineAndAsyncMessageNotificationLogger
    implements PipelineMessageNotificationListener<PipelineMessageNotification> {

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public synchronized void onNotification(PipelineMessageNotification notification) {
    notifications.addLast(notification);
  }

  @Override
  public List getNotifications() {
    return notifications;
  }
}
