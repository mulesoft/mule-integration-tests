/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
  }

  @Test
  public void testReturnPayloadDefaultStrategy() throws Exception {
    try {
      flowRunner("InputService2").withPayload("Test Message").run();
    } catch (MessagingException e) {
      assertThat(e.getCause(), is(instanceOf(FunctionalTestException.class)));
      assertThat(e.getEvent().getMessage().getPayload().getValue(), is(notNullValue()));
    }
  }

  @Test
  public void testReturnPayloadCustomStrategy() throws Exception {
    BaseEvent event = flowRunner("InputService").withPayload("Test Message").run();
    Message msg = event.getMessage();

    assertNotNull(msg);

    assertNotNull(msg.getPayload().getValue());
    assertEquals("Ka-boom!", msg.getPayload().getValue());
  }

}
