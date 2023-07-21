/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class ExceptionStrategyFlowRefTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE = "some message";
  public static final int TIMEOUT = 5000;

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-flow-ref.xml";
  }

  @Test
  public void testExceptionInFlowCalledWithFlowRef() throws Exception {
    flowRunner("exceptionHandlingBlock").runExpectingException();
    Message response = queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(response, notNullValue());
  }
}
