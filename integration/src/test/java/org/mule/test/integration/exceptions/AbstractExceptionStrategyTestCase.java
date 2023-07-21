/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import org.mule.functional.listener.ExceptionListener;
import org.mule.functional.listener.SystemExceptionListener;
import org.mule.test.AbstractIntegrationTestCase;

public abstract class AbstractExceptionStrategyTestCase extends AbstractIntegrationTestCase {

  protected ExceptionListener exceptionListener;
  protected SystemExceptionListener systemExceptionListener;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    exceptionListener = new ExceptionListener(notificationListenerRegistry);
    systemExceptionListener = new SystemExceptionListener(muleContext, notificationListenerRegistry);
  }

}


