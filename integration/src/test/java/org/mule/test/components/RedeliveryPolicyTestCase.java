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

  private void sendDataWeaveObjectMessage() throws Exception {
    flowRunner("redeliveryPolicyFlowDispatch")
        .withPayload("{ \"name\" : \"bruce\"}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .runExpectingException();
  }

}
