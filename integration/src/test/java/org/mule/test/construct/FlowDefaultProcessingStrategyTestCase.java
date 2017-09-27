/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowDefaultProcessingStrategyTestCase extends AbstractIntegrationTestCase {

  private static final String PROCESSOR_THREAD = "processor-thread";
  private static final String FLOW_NAME = "Flow";
  private TestConnectorQueueHandler queueHandler;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  public void requestResponse() throws Exception {
    Message response = flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(response.getPayload().getValue().toString(), is(TEST_PAYLOAD));
    Message message = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

    assertThat(getOutboundProperty(message, PROCESSOR_THREAD), is(not(currentThread().getName())));
  }

  @Test
  public void oneWay() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run();
    Message message = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

    assertThat(getOutboundProperty(message, PROCESSOR_THREAD), is(not(currentThread().getName())));
  }

  @Test
  public void requestResponseTransacted() throws Exception {
    Transaction transaction = createTransactionMock();

    try {
      flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction))
          .run();

      Message message = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

      assertThat(getOutboundProperty(message, PROCESSOR_THREAD), is(currentThread().getName()));
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void oneWayTransacted() throws Exception {
    Transaction transaction = createTransactionMock();

    try {
      flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(
                                                                                                                   transaction))
          .run();

      Message message = queueHandler.read("out", RECEIVE_TIMEOUT).getMessage();

      assertThat(getOutboundProperty(message, PROCESSOR_THREAD), is(currentThread().getName()));
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  private Transaction createTransactionMock() throws TransactionException {
    Transaction transaction = mock(Transaction.class);
    doAnswer((invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(transaction);
      return null;
    })).when(transaction).begin();
    return transaction;
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return CoreEvent.builder(event).message(new TestLegacyMessageBuilder(event.getMessage())
          .addOutboundProperty(PROCESSOR_THREAD, currentThread().getName()).build()).build();
    }
  }

}
