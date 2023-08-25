/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;

public class ExceptionNotificationLogger extends AbstractNotificationLogger<ExceptionNotification>
    implements ExceptionNotificationListener {
  // nothing to do here
}
