/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.core.util.Throwables.getRootCause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.FlowReferenceStory.FLOW_REFERENCE;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Story(FLOW_REFERENCE)
public class FlowRefTestCase extends AbstractIntegrationTestCase {

  private static final String CONTEXT_DEPTH_MESSAGE = "Too many nested child contexts.";

  @Rule
  public ExpectedError expectedException = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  private List<Future<HttpResponse>> sendAsyncs = new ArrayList<>();

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  private Scheduler asyncFlowRunnerScheduler;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-ref.xml";
  }

  @Before
  public void before() {
    sendAsyncs = new ArrayList<>();
    latch = new CountDownLatch(1);
    awaiting.set(0);

    asyncFlowRunnerScheduler = muleContext.getSchedulerService()
        .ioScheduler(muleContext.getSchedulerBaseConfig().withShutdownTimeout(0, SECONDS));

  }

  @After
  public void after() throws Exception {
    asyncFlowRunnerScheduler.shutdownNow();
    latch.countDown();
    for (Future<HttpResponse> sentAsync : sendAsyncs) {
      sentAsync.get(RECEIVE_TIMEOUT, SECONDS);
    }
  }

  @Test
  public void twoFlowRefsToSubFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("flow1").withPayload("0").run();
    assertThat(getPayloadAsString(muleEvent.getMessage()), is("012xyzabc312xyzabc3"));
  }

  @Test
  public void dynamicFlowRef() throws Exception {
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "A").run().getMessage().getPayload().getValue(),
               is("0A"));
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "B").run().getMessage().getPayload().getValue(),
               is("0B"));
  }

  @Test
  public void dynamicFlowRefTextPlain() throws Exception {
    assertThat(flowRunner("flow3").withPayload("0").withVariable("letter", " A ", TEXT_STRING).run().getMessage().getPayload()
        .getValue(),
               is("0A"));
    assertThat(flowRunner("flow3").withPayload("0").withVariable("letter", " B ", TEXT_STRING).run().getMessage().getPayload()
        .getValue(),
               is("0B"));
  }

  @Test
  public void dynamicFlowRefWithChoice() throws Exception {
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "C").run().getMessage().getPayload().getValue(),
               is("0A"));
  }

  @Test
  public void flowRefTargetToFlow() throws Exception {
    assertThat(flowRunner("targetToFlow").run().getVariables().get("flowRefResult").getValue(), is("result"));
  }

  @Test
  public void flowRefTargetToSubFlow() throws Exception {
    assertThat(flowRunner("targetToSubFlow").run().getVariables().get("flowRefResult").getValue(), is("result"));
  }

  @Test
  public void dynamicFlowRefWithScatterGather() throws Exception {
    Map<String, Message> messageList =
        (Map<String, Message>) flowRunner("flow2").withPayload("0").withVariable("letter", "SG").run().getMessage()
            .getPayload().getValue();

    List payloads = messageList.values().stream().map(msg -> msg.getPayload().getValue()).collect(toList());
    assertEquals("0A", payloads.get(0));
    assertEquals("0B", payloads.get(1));
  }

  @Test
  public void flowRefNotFound() throws Exception {
    expectedException.expectMessage(containsString("No flow/sub-flow with name 'sub-flow-Z' found"));
    expectedException.expectErrorType(CORE_NAMESPACE_NAME, ROUTING_ERROR_IDENTIFIER);
    assertThat(flowRunner("flow2").withPayload("0").withVariable("letter", "Z").run().getMessage().getPayload().getValue(),
               is("0C"));
  }

  @Test
  @Issue("MULE-14285")
  public void flowRefFlowErrorNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = synchronizedList(new ArrayList<>());
    setupMessageProcessorNotificationListener(notificationList);

    assertThat(flowRunner("flowRefFlowErrorNotifications").runExpectingException().getCause(),
               instanceOf(IllegalStateException.class));

    assertNotifications(notificationList, "flowRefFlowErrorNotifications/processors/0");
  }

  @Test
  @Issue("MULE-14285")
  public void flowRefSubFlowErrorNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = synchronizedList(new ArrayList<>());
    setupMessageProcessorNotificationListener(notificationList);

    assertThat(flowRunner("flowRefSubFlowErrorNotifications").runExpectingException().getCause(),
               instanceOf(IllegalStateException.class));

    assertNotifications(notificationList, "flowRefSubFlowErrorNotifications/processors/0");
  }

  private void setupMessageProcessorNotificationListener(List<MessageProcessorNotification> notificationList) {
    muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class,
                                                            MessageProcessorNotification.class);
    muleContext.getNotificationManager().addListener((MessageProcessorNotificationListener) notification -> {
      notificationList.add((MessageProcessorNotification) notification);
    });
  }

  private void assertNotifications(List<MessageProcessorNotification> notificationList, String name) {
    probe(() -> {
      assertThat(notificationList.toString(), notificationList, hasSize(4));

      MessageProcessorNotification preNotification = notificationList.get(0);
      assertThat(preNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_PRE_INVOKE));
      assertThat(preNotification.getComponent().getLocation().getLocation(), equalTo(name));
      assertThat(preNotification.getException(), is(nullValue()));

      MessageProcessorNotification postNotification = notificationList.get(3);
      assertThat(postNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_POST_INVOKE));
      assertThat(postNotification.getComponent().getLocation().getLocation(), equalTo(name));
      assertThat(postNotification.getException().getCause(), instanceOf(IllegalStateException.class));
      assertThat(postNotification.getEvent().getError().isPresent(), is(true));
      assertThat(postNotification.getEvent().getError().get().getCause(), instanceOf(IllegalStateException.class));

      return true;
    });
  }

  @Test
  public void recursive() throws Exception {
    flowRunner("recursiveCaller").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  public void recursiveDynamic() throws Exception {
    flowRunner("recursiveDynamicCaller").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  public void recursiveSubFlow() throws Exception {
    flowRunner("recursiveSubFlowCaller").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  public void crossedRecursiveSubFlow() throws Exception {
    flowRunner("crossedRecursiveSubflow").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  public void tripleCrossedRecursiveSubFlow() throws Exception {
    flowRunner("tripleCrossedRecursiveSubflow").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  public void recursiveSubFlowDynamic() throws Exception {
    flowRunner("recursiveSubFlowDynamicCaller").runExpectingException(hasMessage(containsString(CONTEXT_DEPTH_MESSAGE)));
  }

  @Test
  @Story(BACKPRESSURE)
  @Ignore("How to handle backpressure on flow-ref's is not defined yet, but this test will provide a starting point in the future...")
  public void backpressureFlowRef() throws Exception {
    HttpRequest request =
        HttpRequest.builder()
            .uri(format("http://localhost:%s/backpressureFlowRef?ref=backpressureFlowRefInner", port.getNumber())).method(GET)
            .build();

    int nThreads = (getRuntime().availableProcessors() * 4) + 1;

    for (int i = 0; i < nThreads; ++i) {
      sendAsyncs.add(httpClient.sendAsync(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT * 2).build()));
    }

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() >= getRuntime().availableProcessors() * 2);
    probe(RECEIVE_TIMEOUT, 50, () -> {
      assertThat(httpClient.send(request, HttpRequestOptions.builder().responseTimeout(1000).build()).getStatusCode(),
                 is(SERVICE_UNAVAILABLE.getStatusCode()));
      return true;
    });
  }

  @Test
  @Story(BACKPRESSURE)
  @Ignore("How to handle backpressure on flow-ref's is not defined yet, but this test will provide a starting point in the future...")
  public void backpressureFlowRefSub() throws Exception {
    HttpRequest request =
        HttpRequest.builder()
            .uri(format("http://localhost:%s/backpressureFlowRef?ref=backpressureFlowRefInnerSub", port.getNumber()))
            .method(GET).build();

    int nThreads = (getRuntime().availableProcessors() * 4) + 1;

    for (int i = 0; i < nThreads; ++i) {
      sendAsyncs.add(httpClient.sendAsync(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT * 2).build()));
    }

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() >= getRuntime().availableProcessors() * 2);
    probe(RECEIVE_TIMEOUT, 50, () -> {
      assertThat(httpClient.send(request, HttpRequestOptions.builder().responseTimeout(1000).build()).getStatusCode(),
                 is(SERVICE_UNAVAILABLE.getStatusCode()));
      return true;
    });
  }

  @Test
  @Story(BACKPRESSURE)
  public void backpressureFlowRefMaxConcurrency() throws Exception {
    HttpRequest request =
        HttpRequest.builder()
            .uri(format("http://localhost:%s/backpressureFlowRefMaxConcurrency?ref=backpressureFlowRefInner", port.getNumber()))
            .method(GET)
            .build();

    int nThreads = 2;

    for (int i = 0; i < nThreads; ++i) {
      sendAsyncs.add(httpClient.sendAsync(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT * 2).build()));
    }

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() >= 1);
    assertThat(httpClient.send(request).getStatusCode(), is(SERVICE_UNAVAILABLE.getStatusCode()));
  }

  @Test
  @Story(BACKPRESSURE)
  public void backpressureFlowRefMaxConcurrencySub() throws Exception {
    HttpRequest request =
        HttpRequest.builder()
            .uri(format("http://localhost:%s/backpressureFlowRefMaxConcurrency?ref=backpressureFlowRefInnerSub",
                        port.getNumber()))
            .method(GET)
            .build();

    int nThreads = 2;

    for (int i = 0; i < nThreads; ++i) {
      sendAsyncs.add(httpClient.sendAsync(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT * 2).build()));
    }

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() >= 1);
    assertThat(httpClient.send(request).getStatusCode(), is(SERVICE_UNAVAILABLE.getStatusCode()));
  }

  @Test
  public void flowWithStoppedTargetFlowFailsToProcess() throws Exception {
    flowRunner("stoppedTargetFlow1").runExpectingException(ErrorTypeMatcher.errorType("MULE", "UNKNOWN"));
  }

  private void testRecursiveFlowrefsAreDetectedFor(String callingFlowName, String offendingFlowName) {
    try {
      // This will attempt to start the flow. That's the moment the subscription is triggered from downstream, and that's where
      // the inter-flow-ref cycle is checked.
      flowRunner(callingFlowName);
      fail("Expected and error regarding a flowref cycle from " + callingFlowName + ", and with the offending flow being "
          + offendingFlowName);
    } catch (Exception e) {
      Throwable rootCause = getRootCause(e);
      assertThat(rootCause.getMessage(),
                 endsWith(format("Found a possible infinite recursion involving flow named %s", offendingFlowName)));
    }
  }

  @Test
  @Issue("MULE-18178")
  @Story(BACKPRESSURE)
  @Description("The maxConcurrency of a target flow called via flow-ref is enforced")
  public void backpressureFlowRefMaxConcurrencyStatic() throws Exception {
    flowRunner("backpressureFlowRefOuterMaxConcurrencyStatic").dispatchAsync(asyncFlowRunnerScheduler);

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() == 1);

    flowRunner("backpressureFlowRefOuterMaxConcurrencyStatic").dispatchAsync(asyncFlowRunnerScheduler);
    Thread.sleep(RECEIVE_TIMEOUT);
    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() == 1);
    latch.countDown();

    probe(RECEIVE_TIMEOUT, 50, () -> awaiting.get() == 2);
  }

  @Test
  @Issue("MULE-18304")
  @Description("Verify that operations inner fluxes are not terminated when within a dynamically invoked sub-flow.")
  public void dynamicFlowRefWithSdkOperation() throws Exception {
    flowRunner("dynamicFlowRefWithSdkOperation").run();
    flowRunner("dynamicFlowRefWithSdkOperation").run();
  }

  @Test
  @Issue("MULE-19319")
  @Description("For each with a flow ref and max concurrency finish processing")
  public void forEachWithFlowRefAndMaxConcurrency() throws Exception {
    Integer[] payload = new Integer[] {1, 2, 3};
    assertThat(flowRunner("foreachWithFlowRefAndMaxConcurrency").withPayload(payload).run().getMessage()
        .getPayload()
        .getValue(), is(payload));
  }

  private static CountDownLatch latch;
  private static AtomicInteger callbackInFlight = new AtomicInteger();
  private static AtomicInteger awaiting = new AtomicInteger();

  public static class LatchAwaitCallback extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      callbackInFlight.incrementAndGet();
      awaiting.incrementAndGet();
      latch.await();
      callbackInFlight.decrementAndGet();
    }

  }

  public static int getCallbackInFlight() {
    return callbackInFlight.get();
  }
}
