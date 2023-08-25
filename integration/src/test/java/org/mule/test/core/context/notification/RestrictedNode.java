/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
