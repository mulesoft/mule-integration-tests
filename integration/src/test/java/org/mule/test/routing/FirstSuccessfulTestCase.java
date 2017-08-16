/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.extension.validation.api.ValidationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FirstSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "first-successful-test.xml";
  }

  @Test
  public void firstSuccessful() throws Exception {
    Message response = flowRunner("test-router").withPayload("XYZ").run().getMessage();
    assertThat(getPayloadAsString(response), is("XYZ is a string"));

    response = flowRunner("test-router").withPayload(Integer.valueOf(9)).run().getMessage();
    assertThat(getPayloadAsString(response), is("9 is a number"));

    response = flowRunner("test-router").withPayload(Long.valueOf(42)).run().getMessage();
    assertThat(getPayloadAsString(response), is("42 is a number"));

    expected.expectCause(instanceOf(ValidationException.class));
    flowRunner("test-router").withPayload(Boolean.TRUE).run().getMessage();
  }

  @Test
  public void firstSuccessfulWithOneWayEndpoints() throws Exception {
    flowRunner("withOneWayEndpoints").withPayload(TEST_MESSAGE).run();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://WithOneWayEndpoints.out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertThat(response.getPayload().getValue(), is(TEST_MESSAGE));
  }
}
