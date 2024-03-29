/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification.Action;

/**
 * An implementation detail - this enforces the guarantee that once {@link Node#serial(RestrictedNode)} is called,
 * {@link Node#parallel(RestrictedNode)} cannot be.
 */
public interface RestrictedNode {

  public RestrictedNode serial(RestrictedNode node);

  public int match(AbstractServerNotification notification);

  public boolean contains(Class clazz, Action action);

  public boolean isExhausted();

  /**
   * @return Any remaining node
   */
  public RestrictedNode getAnyRemaining();

  Class getNotificationClass();

}
