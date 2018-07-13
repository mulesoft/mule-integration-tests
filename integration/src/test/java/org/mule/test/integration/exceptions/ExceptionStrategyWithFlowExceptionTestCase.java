/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.junit4.TestLegacyMessageUtils;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyWithFlowExceptionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-with-flow-exception.xml";
  }

  @Test
  public void testFlowExceptionExceptionStrategy() throws Exception {
    flowRunner("customException").withPayload(TEST_MESSAGE).dispatch();
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    Message message = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

    assertThat(message, is(notNullValue()));
  }

  public static class ExceptionThrower implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new DefaultMuleException(new Exception());
    }
  }
}
