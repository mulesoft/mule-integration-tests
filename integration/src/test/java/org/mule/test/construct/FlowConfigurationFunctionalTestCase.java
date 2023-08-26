/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class FlowConfigurationFunctionalTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_ARRAY_IN_ARGS_RESULT = "testtestrecieved";

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
  }

  @Test
  public void testFlow() throws Exception {
    final Flow flow = registry.<Flow>lookupByName("flow").get();
    assertEquals(5, flow.getProcessors().size());

    assertEquals("012xyzabc3", getPayloadAsString(flowRunner("flow").withPayload("0").run().getMessage()));
  }

  @Test
  public void testAsyncAsynchronous() throws Exception {
    flowRunner("asynchronousAsync").withPayload("0").run();
    Message message = queueManager.read("asynchronous-async-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertNotSame(currentThread(), thread);
  }

  @Test
  public void testInOutFlow() throws Exception {
    flowRunner("inout").withPayload("0").run();
    assertEquals("0", getPayloadAsString(queueManager.read("inout-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage()));
  }

  @Test
  public void testInOutAppendFlow() throws Exception {
    flowRunner("inout-append").withPayload("0").run();
    assertEquals("0inout", getPayloadAsString(queueManager.read("inout-append-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage()));
  }

  @Test
  public void testComponentsFlow() throws Exception {
    final Message result = flowRunner("components").withPayload("0").run().getMessage();

    assertNotNull(result);
    assertNotSame(TEST_MESSAGE + "test", result.getPayload().getValue());
  }

  @Test
  public void testAsyncOneWayEndpoint() throws Exception {
    flowRunner("async-oneway").withPayload("0").run();
    final Message result = queueManager.read("async-oneway-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    final Message asyncResult = queueManager.read("async-async-oneway-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testAsyncRequestResponseEndpoint() throws Exception {
    CoreEvent syncResult = flowRunner("async-requestresponse").withPayload("0").run();
    final Message result = queueManager.read("async-requestresponse-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    final Message asyncResult = queueManager.read("async-async-requestresponse-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    assertAsync(syncResult.getMessage(), result, asyncResult);
  }

  @Test
  public void testAsyncTransactionalEndpoint() throws Exception {
    Transaction transaction = mock(Transaction.class);
    doAnswer((invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(transaction);
      return null;
    })).when(transaction).begin();

    try {
      CoreEvent syncResult =
          flowRunner("async-tx").withPayload("0").transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction))
              .run();


      final Message result = queueManager.read("async-tx-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
      final Message asyncResult = queueManager.read("async-async-tx-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

      assertAsync(syncResult.getMessage(), result, asyncResult);
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  private void assertAsync(Message syncResult, Message result, Message asyncResult) throws Exception {
    assertNotNull(syncResult);
    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(syncResult));
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testChoiceWithoutOutboundEndpoints() throws Exception {
    assertEquals("foo Hello foo", getPayloadAsString(flowRunner("choice2").withPayload("foo").run().getMessage()));
    assertEquals("bar Hello bar", getPayloadAsString(flowRunner("choice2").withPayload("bar").run().getMessage()));
    assertEquals("egh Hello ?", getPayloadAsString(flowRunner("choice2").withPayload("egh").run().getMessage()));
  }

  @Test
  public void testFlowRef() throws Exception {
    final Message message = flowRunner("flow-ref").withPayload("0").run().getMessage();
    assertEquals("012xyzabc312xyzabc3", getPayloadAsString(message));
  }

  @Test
  public void testLoggerMessage() throws Exception {
    flowRunner("loggermessage").withPayload("0").run();
  }

  @Test
  public void testLoggerHeader() throws Exception {
    flowRunner("loggerheader").withPayload("0").withVariable("toLog", "valueToLog").run();
  }

  public static class Pojo {

    public void method() {
      // does nothing
    }

    public void method(Object arg1, Object arg2) {
      // does nothing
    }
  }

  @Test
  public void testPoll() throws Exception {
    Message message = queueManager.read("poll-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(message);
    assertEquals(" Hello fooout", getPayloadAsString(message));
  }

  @Test
  public void testPollFlowRef() throws Exception {
    Message message = queueManager.read("poll2-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(message);
    assertEquals("nullpollappendout", getPayloadAsString(message));
  }

  @Test
  public void testSubFlowMessageFilter() throws Exception {
    flowRunner("messagefiltersubflow").withPayload("0").run();
    Message message = queueManager.read("messagefiltersubflow-out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(message);
  }

  @Test
  public void customMaxConcurrency() throws Exception {
    Flow flow = registry.<Flow>lookupByName("customMaxConcurrency").get();
    assertThat(flow.getMaxConcurrency(), equalTo(1));
  }

  public static class ThreadSensingMessageProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(currentThread()).build()).build();
    }
  }

}
