/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.sdk.api.error.MuleErrors.CONNECTIVITY;
import static org.mule.tck.processor.FlowAssert.verify;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ForeachStory.FOR_EACH;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.apache.commons.text.RandomStringGenerator;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(FOR_EACH)
public class ForeachTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public SystemProperty systemProperty = new SystemProperty("batch.size", "3");

  @Rule
  public ExpectedError expectedException = none();

  private RandomStringGenerator randomStringGenerator;

  @Before
  public void setUp() {
    randomStringGenerator = new RandomStringGenerator.Builder().withinRange('a', 'z').build();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/routing/foreach-test.xml";
  }

  @Test
  public void defaultConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("julio");
    payload.add("sosa");

    Message result = flowRunner("minimal-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(2));
    assertSame(payload, resultPayload);

    Message out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("julio"));

    out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("sosa"));
  }

  @Test
  public void defaultConfigurationPlusMP() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("syd");
    payload.add("barrett");

    Message result = flowRunner("minimal-config-plus-mp").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    Message out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("syd"));

    out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("barrett"));
  }

  @Test
  public void defaultConfigurationExpression() throws Exception {
    final List<String> names = new ArrayList<>();
    names.add("residente");
    names.add("visitante");

    CoreEvent result = flowRunner("minimal-config-expression").withPayload("message payload")
        .withVariable("names", names).run();

    assertThat(result.getMessage().getPayload().getValue(), instanceOf(String.class));
    assertThat((List<String>) result.getVariables().get("names").getValue(), hasSize(names.size()));

    Message out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("residente"));

    out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("visitante"));
  }

  @Test
  public void partitionedConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("gulp");
    payload.add("oktubre");
    payload.add("un baion");
    payload.add("bang bang");
    payload.add("la mosca");

    Message result = flowRunner("partitioned-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(5));
    assertSame(payload, resultPayload);

    Message out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> outPayload = (Collection<?>) out.getPayload().getValue();
    assertThat(outPayload, hasSize(3));

    out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(Collection.class));
    outPayload = (Collection<?>) out.getPayload().getValue();
    assertThat(outPayload, hasSize(2));
  }

  @Test
  public void rootMessageConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("pyotr");
    payload.add("ilych");

    Message result = flowRunner("parent-message-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(2));
    assertSame(payload, resultPayload);
  }

  @Test
  public void mapPayload() throws Exception {
    final Map<String, String> payload = new HashMap<>();
    payload.put("name", "david");
    payload.put("surname", "bowie");

    Message result = flowRunner("map-config").withPayload(payload).run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(Map.class));
    Map<?, ?> resultPayload = (Map<?, ?>) result.getPayload().getValue();
    assertThat(resultPayload.entrySet(), hasSize(payload.size()));
    assertSame(payload, resultPayload);
  }

  @Test
  public void splitStringWithHardcodedValue() throws Exception {
    flowRunner("splitStringHardcodedValue").run();
    assertSplitedString();
  }

  @Test
  public void splitStringWithStringFromPayload() throws Exception {
    flowRunner("splitStringHardcodedValue").withPayload("a-b-c").run();
    assertSplitedString();
  }

  @Description("Splits a JSON into an array of simple values and iterate over those values")
  @Test
  public void splitJson() throws Exception {
    flowRunner("splitJson").withPayload("{\"name\":\"Pepe\", \"lastname\":\"Le Pew\"}").withMediaType(APPLICATION_JSON).run();
    assertQueueValueIs("splitJsonOutQueue", "Pepe");
    assertQueueValueIs("splitJsonOutQueue", "Le Pew");
  }

  @Description("Splits a JSON array that is inside an string in the payload without using any expression")
  @Test
  public void splitJsonArrayWithoutExpression() throws Exception {
    flowRunner("splitJsonArray").withPayload("[ \"1\", \"2\" ]").withMediaType(APPLICATION_JSON).run();
    assertQueueValueIs("splitJsonArrayOutQueue", "1");
    assertQueueValueIs("splitJsonArrayOutQueue", "2");
  }

  @Description("Splits a JSON into other JSON objects and executed expressions over each object")
  @Test
  public void splitJsonComplexValue() throws Exception {
    String jsonUsers = "{ \"users\": [" +
        "{ \"name\": \"Pepe\", \"lastname\": \"Le Pew\" }," +
        "{ \"name\": \"Chuck\", \"lastname\": \"Jones\" }," +
        "{ \"name\": \"Dick\", \"lastname\": \"Dastardly\" }," +
        "{ \"name\": \"William\", \"lastname\": \"Hanna\" }" +
        "] }";
    flowRunner("splitJsonComplexValue").withVariable("content", jsonUsers, JSON_STRING).withMediaType(APPLICATION_JSON)
        .run();
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Pepe Le Pew");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Chuck Jones");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Dick Dastardly");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "William Hanna");
  }

  @Description("Splits an array of string generated from an expression executed over an XML payload")
  @Test
  public void splitXml() throws Exception {
    flowRunner("splitXml").withPayload("<person><name>Pepe</name><lastname>Le Pew</lastname></person>")
        .withMediaType(APPLICATION_XML).run();
    assertQueueValueIs("splitXmlOutQueue", "Pepe");
    assertQueueValueIs("splitXmlOutQueue", "Le Pew");
  }

  @Description("Splits an XML where root contains a collection of child elements and verifies that expressions over each child element inside the foreach works")
  @Test
  public void spliXmlComplexValue() throws Exception {
    flowRunner("splitXmlComplexValue").withPayload(sampleXml).withMediaType(APPLICATION_XML).run();
    assertQueueValueIs("splitXmlComplexValueOutQueue", "872-AA 140");
    assertQueueValueIs("splitXmlComplexValueOutQueue", "926-AA 35");
  }

  @Description("Splits a collection of Messages and verifies that the Message payload and attribute are set as payload and attributes of the routed message")
  @Test
  public void splitCollectionOfMessages() throws Exception {
    ImmutableList<Message> payload =
        ImmutableList.<Message>builder().add(Message.builder().value("1").attributesValue("one").build())
            .add(Message.builder().value("2").attributesValue("two").build()).build();
    flowRunner("splitPayload").withVariable("useExpression", false).withPayload(payload).withMediaType(APPLICATION_JAVA).run();
    assertQueueValueIs("splitPayloadOutQueue", "1");
    assertQueueValueIs("splitPayloadOutQueue", "2");
  }

  @Description("Splits a collection of Messages with JSON payloads represented as string and verifies that inside the foreach, expressions associated with the json content work fine")
  @Test
  public void splitCollectionOfJsonMessagesAndEachValueHasRightContentType() throws Exception {
    String firstPaylaod = "{ \"name\": \"Pepe\", \"lastname\": \"Le Pew\" }";
    String secondPayload = "{ \"name\": \"Chuck\", \"lastname\": \"Jones\" }";
    ImmutableList<Message> payload =
        ImmutableList.<Message>builder().add(Message.builder().value(firstPaylaod).mediaType(APPLICATION_JSON).build())
            .add(Message.builder().value(secondPayload).mediaType(APPLICATION_JSON).build()).build();
    flowRunner("splitPayload").withVariable("useExpression", true).withPayload(payload).withMediaType(APPLICATION_JAVA).run();
    assertQueueValueIs("splitPayloadOutQueue", "Pepe");
    assertQueueValueIs("splitPayloadOutQueue", "Chuck");
  }

  private void assertQueueValueIs(String queueName, Object queueValue) {
    Message receivedMessage = queueManager.read(queueName, 1000, MILLISECONDS).getMessage();
    assertThat(receivedMessage.getPayload().getValue(), Is.is(queueValue));
  }

  private void assertSplitedString() {
    assertQueueValueIs("splitStringOutQueue", "a");
    assertQueueValueIs("splitStringOutQueue", "b");
    assertQueueValueIs("splitStringOutQueue", "c");
  }

  static String sampleXml = "<PurchaseOrder><Address><Name>Ellen Adams</Name></Address><Items>"
      + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item><Item PartNumber=\"926-AA\"><Price>35</Price></Item>"
      + "</Items></PurchaseOrder>";

  private void xml(Object payload) throws Exception {
    CoreEvent result = flowRunner("process-order-update").withPayload(payload).withMediaType(APPLICATION_XML).run();
    int total = (Integer) result.getVariables().get("total").getValue();
    assertThat(total, is(greaterThan(0)));
  }

  @Ignore("MULE-9285")
  @Issue("MULE-9285")
  @Test
  public void xmlUpdateByteArray() throws Exception {
    xml(sampleXml.getBytes());
  }

  @Test
  public void jsonUpdate() throws Exception {
    List<Object> items = new ArrayList<>();
    items.add(singletonMap("key1", "value1"));
    items.add(singletonMap("key2", "value2"));
    Map<String, Object> order = new HashMap<>();
    order.put("name", "Ellen");
    order.put("email", "ellen.mail.com");
    order.put("items", items);
    expectedException.expectCause(instanceOf(ConcurrentModificationException.class));
    flowRunner("process-json-update").withPayload(singletonMap("order", order)).run();
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] payload = {"uno", "dos", "tres"};

    Message result = flowRunner("array-expression-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(String[].class));
    String[] resultPayload = (String[]) result.getPayload().getValue();
    assertThat(resultPayload, arrayWithSize(payload.length));
    assertSame(payload, resultPayload);
  }

  @Test
  public void variableScope() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("pedro");
    payload.add("rodolfo");
    payload.add("roque");

    flowRunner("counter-scope").withPayload(payload).run();
  }

  @Test
  public void twoOneAfterAnother() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("rosa");
    payload.add("maria");
    payload.add("florencia");

    Message result = flowRunner("counter-two-foreach-independence").withPayload(payload).run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);
  }

  @Test
  public void nestedConfig() throws Exception {
    final List<List<String>> payload = createNestedPayload();

    Message result = flowRunner("nested-foreach").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    Message out;
    for (int i = 0; i < payload.size(); i++) {
      for (int j = 0; j < payload.get(i).size(); j++) {
        out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
        assertThat(out.getPayload().getValue(), instanceOf(String.class));
        assertThat(out.getPayload().getValue(), is(payload.get(i).get(j)));
      }
    }
  }

  @Test
  public void nestedCounters() throws Exception {
    final List<List<String>> payload = createNestedPayload();

    Message result = flowRunner("nested-foreach-counters").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    for (int i = 0; i < payload.size(); i++) {
      for (int j = 0; j < payload.get(i).size(); j++) {
        queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
      }
      queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    }
  }

  private List<List<String>> createNestedPayload() {
    final List<List<String>> payload = new ArrayList<>();
    final List<String> elem1 = new ArrayList<>();
    final List<String> elem2 = new ArrayList<>();
    final List<String> elem3 = new ArrayList<>();
    elem1.add("a1");
    elem1.add("a2");
    elem1.add("a3");
    elem2.add("b1");
    elem2.add("b2");
    elem3.add("c1");
    payload.add(elem1);
    payload.add(elem2);
    payload.add(elem3);

    return payload;
  }

  @Test
  public void propertiesRestored() throws Exception {
    String[] payload = {"uno", "dos", "tres"};

    Message result = flowRunner("foreach-properties-restored").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(String[].class));
    String[] resultPayload = (String[]) result.getPayload().getValue();
    assertThat(resultPayload, arrayWithSize(payload.length));
    assertSame(payload, resultPayload);
  }

  private void assertIterable() {
    Message out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    String outPayload = (String) out.getPayload().getValue();

    assertThat(outPayload, is("foo"));

    out = queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    outPayload = (String) out.getPayload().getValue();
    assertThat(outPayload, is("bar"));
  }

  @Test
  public void expressionIterable() throws Exception {
    Iterable<String> iterable = mock(Iterable.class);
    when(iterable.iterator()).thenReturn(asList("foo", "bar").iterator());
    flowRunner("expression-iterable").withVariable("iterable", iterable).run();

    assertIterable();
  }

  @Test
  public void mvelError() throws Exception {
    MuleException me = (MuleException) flowRunner("errorExpression").runExpectingException();
    assertThat((String) me.getInfo().get(INFO_LOCATION_KEY), startsWith("errorExpression/processors/0 @"));
  }

  @Test
  public void foreachWithAsync() throws Exception {
    final int size = 20;
    List<String> list = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      list.add(randomStringGenerator.generate(10));
    }

    // one more for the flow-ref outside foreach
    CountDownLatch latch = new CountDownLatch(size + 1);
    flowRunner("foreachWithAsync").withPayload(list).withVariable("latch", latch).run();

    assertThat(latch.toString(), latch.await(10, SECONDS), is(true));
  }

  public static CountDownLatch coundDownLatch(CountDownLatch latch) {
    latch.countDown();
    return latch;
  }

  @Test
  public void errorsWithinArePropagated() throws Exception {
    Message message = flowRunner("error-handler").withPayload(new String[] {TEST_PAYLOAD}).run().getMessage();
    assertThat(message, hasPayload(equalTo("handled")));
  }

  @Test
  public void nonBlocking() throws Exception {
    flowRunner("nonBlocking").withPayload(asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")).run();
    verify("nonBlocking");
  }

  @Test
  public void errorAfterThreadChange() throws Exception {
    expectedException.expectErrorType("MULE", CONNECTIVITY.name());
    flowRunner("errorAfterThreadChange").withPayload(asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")).run();
  }

  @Test
  @Description("Validates that for each can be used correctly within an error handler")
  public void foreachInErrorHandler() throws Exception {
    CoreEvent event = flowRunner("foreachInErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("apple"));
  }

  @Test
  @Issue("MULE-18524")
  public void forEachWithTry() throws Exception {
    flowRunner("forEachWithTry").withPayload(ImmutableList.of("one", "two", "three")).run();
  }

  @Test
  @Issue("MULE-18462")
  public void forEachEmptyCollection() throws Exception {
    flowRunner("emptyForEach").run();
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
