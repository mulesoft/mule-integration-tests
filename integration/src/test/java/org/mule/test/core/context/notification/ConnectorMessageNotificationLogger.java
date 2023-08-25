/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.ConnectorMessageNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;
import org.mule.runtime.api.notification.ConnectorMessageNotification;

public class ConnectorMessageNotificationLogger extends AbstractNotificationLogger<ConnectorMessageNotification>
    implements ConnectorMessageNotificationListener<ConnectorMessageNotification> {

  @Override
  public boolean isBlocking() {
    return false;
  }
}
