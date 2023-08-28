/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;

public class ExceptionNotificationLogger extends AbstractNotificationLogger<ExceptionNotification>
    implements ExceptionNotificationListener {
  // nothing to do here
}
