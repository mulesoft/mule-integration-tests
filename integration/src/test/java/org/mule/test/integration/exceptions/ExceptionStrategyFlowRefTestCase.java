/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyFlowRefTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE = "some message";
  public static final int TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-flow-ref.xml";
  }

  @Test
  public void testExceptionInFlowCalledWithFlowRef() throws Exception {
    flowRunner("exceptionHandlingBlock").runExpectingException();
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(muleContext);

    Message response = queueHandler.read("dlq", RECEIVE_TIMEOUT).getMessage();

    assertThat(response, notNullValue());
  }
}
