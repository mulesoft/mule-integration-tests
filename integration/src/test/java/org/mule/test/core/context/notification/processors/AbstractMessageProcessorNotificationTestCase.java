/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification.processors;

import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;

import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.test.core.context.notification.AbstractNotificationTestCase;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

public abstract class AbstractMessageProcessorNotificationTestCase extends AbstractNotificationTestCase {

  protected RestrictedNode pre() {
    return new Node(MessageProcessorNotification.class, new IntegerAction(MESSAGE_PROCESSOR_PRE_INVOKE));
  }

  protected RestrictedNode post() {
    return new Node(MessageProcessorNotification.class, new IntegerAction(MESSAGE_PROCESSOR_POST_INVOKE));
  }

  protected RestrictedNode prePost() {
    return new Node().serial(pre()).serial(post());
  }
}
