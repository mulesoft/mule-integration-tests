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
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;
import static org.mule.functional.junit4.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import org.mule.functional.api.component.SkeletonSource;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
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

  public FlowConfigurationFunctionalTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow.xml";
  }

  @Test
  public void testFlow() throws Exception {
    final Flow flow = muleContext.getRegistry().lookupObject("flow");
    assertEquals(5, flow.getProcessors().size());
    assertNotNull(flow.getExceptionListener());

    assertEquals("012xyzabc3", getPayloadAsString(flowRunner("flow").withPayload("0").run().getMessage()));
  }

  @Test
  public void testFlowSynchronous() throws Exception {
    flowRunner("synchronousFlow").withPayload("0").run();
    Message message = muleContext.getClient().request("test://synchronous-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertEquals(currentThread(), thread);
  }

  @Test
  public void testAsyncAsynchronous() throws Exception {
    flowRunner("asynchronousAsync").withPayload("0").run();
    Message message = muleContext.getClient().request("test://asynchronous-async-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertNotSame(currentThread(), thread);
  }

  @Test
  public void testFlowCompositeSource() throws Exception {
    final Flow flow = muleContext.getRegistry().lookupObject("flow2");
    CompositeMessageSource compositeSource = (CompositeMessageSource) flow.getSource();
    assertThat(compositeSource.getClass().getName(),
               equalTo("org.mule.runtime.core.internal.source.StartableCompositeMessageSource"));
    assertEquals(2, flow.getProcessors().size());

    final List<MessageSource> sources = compositeSource.getSources();
    SkeletonSource source1 = (SkeletonSource) sources.get(0);
    SkeletonSource source2 = (SkeletonSource) sources.get(1);

    assertEquals("01xyz", getPayloadAsString(source1
        .getListener().process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
            .message(of("0"))
            .build())
        .getMessage()));
    assertEquals("01xyz", getPayloadAsString(source2
        .getListener().process(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION))
            .message(of("0"))
            .build())
        .getMessage()));
  }

  @Test
  public void testInOutFlow() throws Exception {
    flowRunner("inout").withPayload("0").run();
    assertEquals("0", getPayloadAsString(muleContext.getClient().request("test://inout-out", RECEIVE_TIMEOUT).getRight().get()));
  }

  @Test
  public void testInOutAppendFlow() throws Exception {
    flowRunner("inout-append").withPayload("0").run();
    MuleClient client = muleContext.getClient();
    assertEquals("0inout", getPayloadAsString(client.request("test://inout-append-out", RECEIVE_TIMEOUT).getRight().get()));
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
        muleContext.getClient().request("test://split-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

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

  @Ignore("MULE-12407")
  @Test
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

    final Message result =
        muleContext.getClient().request("test://split-aggregate-list-out", RECEIVE_TIMEOUT).getRight().get();

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

    final MuleClient client = muleContext.getClient();
    final Message result = client.request("test://split-aggregate-singleton-list-out", RECEIVE_TIMEOUT).getRight().get();

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

    Event result = flowRunner("split-aggregate-map").withPayload(map).run();

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

    final MuleClient client = muleContext.getClient();
    final Message result = client.request("test://split-filter-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

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

    MuleClient client = muleContext.getClient();
    final Message result = client.request("test://message-chunk-split-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

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
    MuleClient client = muleContext.getClient();
    final Message result = client.request("test://async-oneway-out", RECEIVE_TIMEOUT).getRight().get();
    final Message asyncResult = client.request("test://async-async-oneway-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testAsyncRequestResponseEndpoint() throws Exception {
    Event syncResult = flowRunner("async-requestresponse").withPayload("0").run();
    MuleClient client = muleContext.getClient();
    final Message result = client.request("test://async-requestresponse-out", RECEIVE_TIMEOUT).getRight().get();
    final Message asyncResult =
        client.request("test://async-async-requestresponse-out", RECEIVE_TIMEOUT).getRight().get();

    assertAsync(syncResult.getMessage(), result, asyncResult);
  }

  @Test
  public void testAsyncTransactionalEndpoint() throws Exception {
    Event syncResult =
        flowRunner("async-tx").withPayload("0").transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory()).run();

    final MuleClient client = muleContext.getClient();
    final Message result =
        client.request("test://async-tx-out", RECEIVE_TIMEOUT).getRight().get();
    final Message asyncResult =
        client.request("test://async-async-tx-out", RECEIVE_TIMEOUT).getRight().get();

    assertAsync(syncResult.getMessage(), result, asyncResult);
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
    Message message = muleContext.getClient().request("test://poll-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertEquals(" Hello fooout", getPayloadAsString(message));
  }

  @Test
  public void testPollFlowRef() throws Exception {
    Message message = muleContext.getClient().request("test://poll2-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertEquals("nullpollappendout", getPayloadAsString(message));
  }

  @Test
  public void testSubFlowMessageFilter() throws Exception {
    flowRunner("messagefiltersubflow").withPayload("0").run();
    Message message =
        muleContext.getClient().request("test://messagefiltersubflow-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
  }

  @Test
  public void customMaxConcurrency() throws Exception {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("customMaxConcurrency");
    assertThat(flow.getMaxConcurrency(), equalTo(1));
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event).message(Message.builder(event.getMessage()).payload(currentThread()).build()).build();
    }
  }

}
