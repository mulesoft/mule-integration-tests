/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class InOptionalOutOutOnlyTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    FlowRunner baseRunner = flowRunner("In-Optional-Out_Out-Only-Service").withPayload("some data");
    Message result = baseRunner.run().getMessage();

    assertNotNull(result);
    assertThat(result.getPayload().getValue(), is(nullValue()));

    baseRunner.reset();
    result = baseRunner.withVariable("foo", "bar").run().getMessage();

    assertNotNull(result);
    assertThat(result.getPayload().getValue(), is("foo header received"));
  }
}
