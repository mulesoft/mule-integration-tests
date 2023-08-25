/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;

public class PipelineMessageNotificationLogger extends AbstractNotificationLogger<PipelineMessageNotification>
    implements PipelineMessageNotificationListener<PipelineMessageNotification> {

  @Override
  public boolean isBlocking() {
    return false;
  }

}
