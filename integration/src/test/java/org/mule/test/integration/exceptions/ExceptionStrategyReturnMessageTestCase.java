/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
  }

  @Test
  public void testReturnPayloadDefaultStrategy() throws Exception {
    flowRunner("InputService2").withPayload("Test Message")
        .runExpectingException(instanceOf(FunctionalTestException.class), hasMessage(hasPayload(not(nullValue(String.class)))));
  }

  @Test
  public void testReturnPayloadCustomStrategy() throws Exception {
    CoreEvent event = flowRunner("InputService").withPayload("Test Message").run();
    Message msg = event.getMessage();

    assertNotNull(msg);

    assertNotNull(msg.getPayload().getValue());
    assertEquals("Ka-boom!", msg.getPayload().getValue());
  }

}
