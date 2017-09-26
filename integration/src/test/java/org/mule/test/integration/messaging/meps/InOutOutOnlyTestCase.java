/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class InOutOutOnlyTestCase extends AbstractIntegrationTestCase {

  private static TestConnectorQueueHandler queueHandler;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Out_Out-Only-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  public void testExchangeReceived() throws Exception {

    Message result = flowRunner("In-Out_Out-Only-Service").withPayload("some data").withVariable("foo", "bar").run().getMessage();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header received"));

    result = queueHandler.read("received", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header received"));
  }

  @Test
  public void testExchangeNotReceived() throws Exception {
    Message result = flowRunner("In-Out_Out-Only-Service").withPayload("some data").run().getMessage();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header not received"));

    result = queueHandler.read("notReceived", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header not received"));
  }
}
