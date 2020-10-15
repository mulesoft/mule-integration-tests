/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class FlowConfigurationFunctionalTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_ARRAY_IN_ARGS_RESULT = "testtestrecieved";
  private TestConnectorQueueHandler queueHandler;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  public void testFlow() throws Exception {
    final Flow flow = registry.<Flow>lookupByName("flow").get();
    assertEquals(5, flow.getProcessors().size());
    assertNotNull(flow.getExceptionListener());

    assertEquals("012xyzabc3", getPayloadAsString(flowRunner("flow").withPayload("0").run().getMessage()));
  }

  @Test
  public void testAsyncAsynchronous() throws Exception {
    flowRunner("asynchronousAsync").withPayload("0").run();
    Message message = queueHandler.read("asynchronous-async-out", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertNotSame(currentThread(), thread);
  }

  @Test
  public void testInOutFlow() throws Exception {
    flowRunner("inout").withPayload("0").run();
    assertEquals("0", getPayloadAsString(queueHandler.read("inout-out", RECEIVE_TIMEOUT).getMessage()));
  }

  @Test
  public void testInOutAppendFlow() throws Exception {
    flowRunner("inout-append").withPayload("0").run();
    assertEquals("0inout", getPayloadAsString(queueHandler.read("inout-append-out", RECEIVE_TIMEOUT).getMessage()));
  }

  @Test
  public void testSplitAggregateFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-aggregate").withPayload(fruitBowl).run();

    final Message result =
        queueHandler.read("split-aggregate-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  @Ignore("MULE-12407, MULE-17630")
  public void testSplitNoParts() throws Exception {
    String MESSAGE = "<Order></Order>";
    Message result = flowRunner("split-no-parts").withPayload(MESSAGE).withMediaType(APPLICATION_XML).run().getMessage();

    assertNotNull(result);
    assertEquals(result.getPayload().getValue(), MESSAGE);
  }

  @Test
  public void testSplitAggregateListFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-aggregate-list").withPayload(fruitBowl.getFruit()).run();

    final Message result = queueHandler.read("split-aggregate-list-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  public void testSplitAggregateListFlowSingleItem() throws Exception {
    final Apple apple = new Apple();
    final FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);

    flowRunner("split-aggregate-singleton-list").withPayload(fruitBowl.getFruit()).run();

    final Message result = queueHandler.read("split-aggregate-singleton-list-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());

    assertTrue(results.contains(apple));
  }

  @Test
  public void testSplitAggregateResponseListFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    final Message result =
        flowRunner("split-aggregate-response-list").withPayload(fruitBowl.getFruit()).run().getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  public void testSplitAggregateResponseListFlowSingleItem() throws Exception {
    final Apple apple = new Apple();
    final FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);

    final Message result =
        flowRunner("split-aggregate-response-singleton-list").withPayload(fruitBowl.getFruit()).run().getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(results.contains(apple));
  }

  @Test
  public void testSplitAggregateMapFlow() throws Exception {
    Map<String, Fruit> map = new HashMap<>();
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    map.put("apple", apple);
    map.put("banana", banana);
    map.put("orange", orange);

    CoreEvent result = flowRunner("split-aggregate-map").withPayload(map).run();

    assertNotNull(result);
    assertTrue(result.getMessage().getPayload().getValue() instanceof List);
    final Message[] results = new Message[3];
    ((List<Message>) result.getMessage().getPayload().getValue()).toArray(results);
    assertEquals(3, results.length);

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());
  }

  @Test
  public void testSplitFilterAggregateFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-filter-aggregate").withPayload(fruitBowl).run();

    final Message result = queueHandler.read("split-filter-aggregate-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<Message> coll = (List<Message>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(results.contains(apple));
    assertFalse(results.contains(banana));
    assertFalse(results.contains(orange));
  }

  @Test
  public void testMessageChunkSplitAggregateFlow() throws Exception {
    String payload = "";
    for (int i = 0; i < 100; i++) {
      payload += TEST_MESSAGE;
    }

    flowRunner("message-chunk-split-aggregate").withPayload(payload).run();

    final Message result = queueHandler.read("message-chunk-split-aggregate-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertNotSame(payload, result.getPayload().getValue());
    assertEquals(payload, getPayloadAsString(result));
  }

  @Test
  public void testComponentsFlow() throws Exception {
    final Message result = flowRunner("components").withPayload("0").run().getMessage();

    assertNotNull(result);
    assertNotSame(TEST_MESSAGE + "test", result.getPayload().getValue());
  }

  @Test
  public void testResponseElement() throws Exception {
    final Message result = flowRunner("response").withPayload("").run().getMessage();

    assertNotNull(result);
    assertEquals("abcdefghi", getPayloadAsString(result));
  }

  @Test
  public void testAsyncOneWayEndpoint() throws Exception {
    flowRunner("async-oneway").withPayload("0").run();
    final Message result = queueHandler.read("async-oneway-out", RECEIVE_TIMEOUT).getMessage();
    final Message asyncResult = queueHandler.read("async-async-oneway-out", RECEIVE_TIMEOUT).getMessage();

    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testAsyncRequestResponseEndpoint() throws Exception {
    CoreEvent syncResult = flowRunner("async-requestresponse").withPayload("0").run();
    final Message result = queueHandler.read("async-requestresponse-out", RECEIVE_TIMEOUT).getMessage();
    final Message asyncResult =
        queueHandler.read("async-async-requestresponse-out", RECEIVE_TIMEOUT).getMessage();

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


      final Message result =
          queueHandler.read("async-tx-out", RECEIVE_TIMEOUT).getMessage();
      final Message asyncResult =
          queueHandler.read("async-async-tx-out", RECEIVE_TIMEOUT).getMessage();

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
  public void testInvoke() throws Exception {
    final Message message = flowRunner("invoke").withPayload("0").run().getMessage();
    assertEquals("0recieved", getPayloadAsString(message));
  }

  @Test
  public void testInvoke2() throws Exception {
    final Message response =
        flowRunner("invoke2").withPayload("0").withInboundProperty("one", "header1val").run().getMessage();
    assertEquals("header1valrecieved", getPayloadAsString(response));
  }

  @Test
  public void testInvoke3() throws Exception {
    // ensure multiple arguments work
    flowRunner("invoke3").withPayload("0").run();
  }

  @Test
  public void testInvoke4() throws Exception {
    // ensure no arguments work
    flowRunner("invoke4").withPayload("0").run();
  }

  @Test
  public void testInvokeArrayInArgs() throws Exception {
    final Message message = flowRunner("invokeArrayInArgs").withPayload("0").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(EXPECTED_ARRAY_IN_ARGS_RESULT));
  }

  @Test
  public void testEnrichWithAttributes() throws Exception {
    final Message muleMessage = flowRunner("enrich").withPayload("0").run().getMessage();
    assertEquals("0Hello", getOutboundProperty(muleMessage, "helloHeader"));
  }

  @Test
  public void testEnrichWithElements() throws Exception {
    Message result = flowRunner("enrich2").withPayload("0").run().getMessage();

    assertEquals("0Hello", getOutboundProperty(result, "helloHeader"));
    assertEquals("0Hello", getOutboundProperty(result, "helloHeader2"));
  }

  @Test
  public void testEnrichUsingComponent() throws Exception {
    // MULE-5544
    Message result = flowRunner("enrichcomponent").withPayload("0").run().getMessage();

    assertEquals("0", getOutboundProperty(result, "echoHeader"));
  }

  @Test
  public void testEnrichUsingComponent2() throws Exception {
    // MULE-5544
    Message result = flowRunner("enrichcomponent2").withPayload("0").run().getMessage();

    assertEquals("0", getOutboundProperty(result, "echoHeader"));
  }

  @Test
  public void testLoggerMessage() throws Exception {
    flowRunner("loggermessage").withPayload("0").run();
  }

  @Test
  public void testLoggerHeader() throws Exception {
    flowRunner("loggerheader").withPayload("0").withOutboundProperty("toLog", "valueToLog").run();
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
    Message message = queueHandler.read("poll-out", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(message);
    assertEquals(" Hello fooout", getPayloadAsString(message));
  }

  @Test
  public void testPollFlowRef() throws Exception {
    Message message = queueHandler.read("poll2-out", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(message);
    assertEquals("nullpollappendout", getPayloadAsString(message));
  }

  @Test
  public void testSubFlowMessageFilter() throws Exception {
    flowRunner("messagefiltersubflow").withPayload("0").run();
    Message message =
        queueHandler.read("messagefiltersubflow-out", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(message);
  }

  @Test
  public void customMaxConcurrency() throws Exception {
    Flow flow = registry.<Flow>lookupByName("customMaxConcurrency").get();
    assertThat(flow.getMaxConcurrency(), equalTo(1));
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(currentThread()).build()).build();
    }
  }

  public static class CustomAppender extends StringAppendTransformer {

    public CustomAppender() {
      super("recieved");
    }

  }

}
