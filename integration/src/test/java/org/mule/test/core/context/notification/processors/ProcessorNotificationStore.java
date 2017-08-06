/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.mule.runtime.core.api.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;

import org.mule.runtime.core.api.context.notification.IntegerAction;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotificationListener;
import org.mule.tck.core.context.notification.AbstractNotificationLogger;

public class ProcessorNotificationStore extends AbstractNotificationLogger<MessageProcessorNotification>
    implements MessageProcessorNotificationListener<MessageProcessorNotification> {

  boolean logSingleNotification = false;

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public synchronized void onNotification(MessageProcessorNotification notification) {
    if (!logSingleNotification || new IntegerAction(MESSAGE_PROCESSOR_PRE_INVOKE).equals(notification.getAction())) {
      super.onNotification(notification);
    }
  }

  public void setLogSingleNotification(boolean logSingleNotification) {
    this.logSingleNotification = logSingleNotification;
  }
}
