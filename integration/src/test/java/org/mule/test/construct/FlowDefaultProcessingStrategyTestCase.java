/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class FlowDefaultProcessingStrategyTestCase extends AbstractIntegrationTestCase {

  private static final String FLOW_NAME = "Flow";

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
  }

  @Test
  public void requestResponse() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run();
    Message message = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertThat(message.getPayload().getValue(), is(not(currentThread().getName())));
  }

  @Test
  public void oneWay() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run();
    Message message = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertThat(message.getPayload().getValue(), is(not(currentThread().getName())));
  }

  @Test
  public void requestResponseTransacted() throws Exception {
    Transaction transaction = createTransactionMock();

    try {
      flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction))
          .run();

      Message message = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

      assertThat(message.getPayload().getValue(), is(currentThread().getName()));
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

      Message message = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

      assertThat(message.getPayload().getValue(), is(currentThread().getName()));
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
      return CoreEvent.builder(event)
          .message(Message.builder(event.getMessage()).payload(new TypedValue<>(currentThread().getName(), STRING)).build())
          .build();
    }
  }

}
