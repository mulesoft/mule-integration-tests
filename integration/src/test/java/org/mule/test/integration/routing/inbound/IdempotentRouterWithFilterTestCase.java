/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.inbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class IdempotentRouterWithFilterTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/inbound/idempotent-router-with-filter-flow.xml";
  }

  /**
   * This test will pass a message containing a String to the Mule server and verifies that it gets received.
   * 
   * @throws Exception
   */
  @Test
  @SuppressWarnings("null")
  public void testWithValidData() throws Exception {
    flowRunner("IdempotentPlaceHolder").withPayload("Mule is the best!").run();
    Message response = queueManager.read("ToTestCase", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertNotNull(response);
    assertNotNull(response.getPayload().getValue());
    assertThat(response.getPayload().getValue(), is("Mule is the best!"));
  }

  /**
   * This test will pass a message containing an Object to the Mule server and verifies that it does not get received.
   * 
   * @throws Exception
   */
  @Test
  public void testWithInvalidData() throws Exception {
    flowRunner("IdempotentPlaceHolder").withPayload(new Object()).run();
    assertThat(queueManager.read("ToTestCase", RECEIVE_TIMEOUT, MILLISECONDS), is(nullValue()));
  }
}
