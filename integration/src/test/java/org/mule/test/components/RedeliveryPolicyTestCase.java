/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.AbstractIntegrationTestCase;

public class RedeliveryPolicyTestCase extends AbstractIntegrationTestCase {

  private TestConnectorQueueHandler queueHandler;

  @Before
  public void setUp() {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/redelivery-policy-config.xml";
  }

  @Test
  public void hashWorksOverDataWeaveObject() throws Exception {
    sendDataWeaveObjectMessage();
    sendDataWeaveObjectMessage();
    assertThat(queueHandler.read("redeliveredMessageQueue", RECEIVE_TIMEOUT), notNullValue());
  }

  @Test
  public void javaPojoPayload() throws Exception {
    final PojoPayload pojoPayload = new PojoPayload();
    flowRunner("redeliveryPolicy3FlowDispatch")
        .withPayload(pojoPayload)
        .withMediaType(APPLICATION_JAVA)
        .run();

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    assertThat(queueHandler.read("processed", RECEIVE_TIMEOUT), notNullValue());

    assertThat(pojoPayload.isHashCodeCalled(), is(true));
  }

  private static class PojoPayload {

    private boolean hashCodeCalled = false;

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      hashCodeCalled = true;
      return super.hashCode();
    }

    public boolean isHashCodeCalled() {
      return hashCodeCalled;
    }
  }

  private void sendDataWeaveObjectMessage() throws Exception {
    flowRunner("redeliveryPolicyFlowDispatch")
        .withPayload("{ \"name\" : \"bruce\"}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .runExpectingException();
  }

}
