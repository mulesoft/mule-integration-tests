/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import org.junit.Test;

import jakarta.inject.Inject;

public class InOnlyOptionalOutTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Only_Optional-Out-flow.xml";
  }

  @Test
  public void testExchangeReceived() throws Exception {
    flowRunner("In-Only_Optional-Out--Service").withPayload("some data").withVariable("foo", "bar").run();

    Message result = queueManager.read("received", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(result);
    assertThat(getPayloadAsString(result), is("foo header received"));
  }

  @Test
  public void testExchangeNotReceived() throws Exception {
    flowRunner("In-Only_Optional-Out--Service").withPayload("some data").run();

    assertThat(queueManager.read("notReceived", RECEIVE_TIMEOUT, MILLISECONDS), is(nullValue()));
  }
}
