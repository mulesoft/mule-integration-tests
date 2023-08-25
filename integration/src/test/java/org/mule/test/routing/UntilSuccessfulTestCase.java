/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.routing;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ClassNameMatcher.hasClassName;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
public class UntilSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FunctionalTestProcessor targetMessageProcessor;

  @Override
  protected String getConfigFile() {
    return "until-successful-test.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    targetMessageProcessor = getFromFlow(locator, "target-mp");
  }

  @Override
  protected void doTearDown() throws Exception {
    CustomMP.clearCount();

    super.doTearDown();
  }

  @Test
  public void executesOnceWhenNoErrorArises() throws Exception {
    CoreEvent response = flowRunner("happy-path-scope").run();
    assertThat(getPayloadAsString(response.getMessage()), is("pig"));
    assertThat(queueManager.countPendingEvents("insideScope"), is(1));
  }

  @Test
  public void nestedUntilSuccessfulScopesExecutionTimes() throws Exception {
    CoreEvent response = flowRunner("nestedUntilSuccessfulScopes").run();
    assertThat(getPayloadAsString(response.getMessage()), is("holis"));
    // Each scope was executed desired amount of times
    assertThat(queueManager.countPendingEvents("outerScope"), is(2));
    assertThat(queueManager.countPendingEvents("innerScope"), is(6));
  }

  @Test
  public void scopeContinuesWorkingAfterErrorInMaxRetryExpression() throws Exception {
    for (int i = 0; i < getRuntime().availableProcessors() * 2; i++) {
      flowRunner("scope-with-max-retry-expr-and-err-continue").withVariable("maxRetries", "gato").runExpectingException();
    }
    CoreEvent response =
        flowRunner("scope-with-max-retry-expr-and-err-continue").withVariable("maxRetries", "1").run();
    assertThat(getPayloadAsString(response.getMessage()), is("holis"));
  }

  @Test
  public void exceptionThrownFromInitializingRetryContextPropagates() throws Exception {
    expectedException.expectMessage(containsString("You called the function '+' with these arguments"));
    expectedException.expectCause(instanceOf(ExpressionRuntimeException.class));
    flowRunner("scope-with-max-retry-expr-and-err-continue").withVariable("maxRetries", "gato").run();
  }

  @Test
  public void scopeHappyPathWithDifferentPayloads() throws Exception {
    assertThat(getPayloadAsString(flowRunner("us-with-no-errors").withPayload("perro").run().getMessage()), is("perro holis"));
    assertThat(getPayloadAsString(flowRunner("us-with-no-errors").withPayload("gato").run().getMessage()), is("gato holis"));
  }

  @Test
  public void defaultConfiguration() throws Exception {
    final String payload = randomAlphanumeric(20);
    flowRunner("minimal-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
    assertThat(receivedPayloads, hasSize(1));
    assertThat(receivedPayloads.get(0), is(payload));
  }

  @Test
  public void fullConfigurationMP() throws Exception {
    final String payload = randomAlphanumeric(20);
    final Message response = flowRunner("full-config-with-mp").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is("ACK"));

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }

    ponderUntilMessageCountReceivedByCustomMP(1);

    Error error = CustomMP.getProcessedEvents().get(0).getError().get();

    assertThat(error.getErrorType().toString(), equalTo("MULE:RETRY_EXHAUSTED"));
    assertThat(error.getDescription(), equalTo("Value was expected to be false but it was true instead"));
    assertThat(error.getDetailedDescription(), equalTo("'until-successful' retries exhausted"));

    Throwable errorCause = error.getCause();
    assertThat(errorCause, is(notNullValue()));
    assertThat(errorCause, instanceOf(RetryPolicyExhaustedException.class));
  }

  @Test
  public void withConcurrency() throws Exception {
    final int times = getRuntime().availableProcessors() * 2;
    final ExecutorService runnerPool = newFixedThreadPool(times);
    try {
      for (int i = 0; i < times; ++i) {
        runnerPool.submit(() -> {
          try {
            flowRunner("concurrency-error-handling").withPayload(randomAlphanumeric(20)).run();
          } catch (Exception e) {
            throw new MuleRuntimeException(e);
          }
        });
      }

      ponderUntilMessageCountReceivedByCustomMP(times);
    } finally {
      runnerPool.shutdownNow();
    }
  }

  @Test
  public void retryOnEndpoint() throws Exception {
    final String payload = randomAlphanumeric(20);
    flowRunner("retry-endpoint-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }
  }

  @Test
  public void executeSynchronously() throws Exception {
    final String payload = randomAlphanumeric(20);
    expectedException.expectCause(instanceOf(RetryPolicyExhaustedException.class));
    expectedException.expect(hasCause(hasCause(hasClassName(containsString("SuppressedMuleException")))));
    flowRunner("synchronous").withPayload(payload).run();
  }

  @Test
  public void executeSynchronouslyDoingRetries() throws Exception {
    final String payload = randomAlphanumeric(20);
    flowRunner("synchronous-with-retry").withPayload(payload).runExpectingException();
    assertThat(queueManager.countPendingEvents("untilSuccessful"), is(4));
    assertThat(queueManager.countPendingEvents("exceptionStrategy"), is(1));
  }

  @Test
  public void executeSynchronouslyDoingExpressionRetries() throws Exception {
    flowRunner("synchronous-with-expression-retry").runExpectingException();
    assertThat(queueManager.countPendingEvents("untilSuccessfulExpression"), is(6));
    assertThat(queueManager.countPendingEvents("exceptionStrategyExpression"), is(1));
  }

  @Test
  public void executeWithoutRetrying() throws Exception {
    final String payload = randomAlphanumeric(20);
    flowRunner("synchronous-without-retry").withPayload(payload).runExpectingException();
    assertThat(queueManager.countPendingEvents("untilSuccessfulNoRetry"), is(1));
    assertThat(queueManager.countPendingEvents("exceptionStrategyNoRetry"), is(1));
  }

  /**
   * Verifies that the synchronous wait time is consistent with that requested
   */
  @Test
  public void measureSynchronousWait() throws Exception {
    final String payload = randomAlphanumeric(20);
    flowRunner("measureSynchronousWait").withPayload(payload).runExpectingException();
    assertThat(WaitMeasure.totalWait >= 1000, is(true));
  }

  @Test
  @Description("Validates that until successful can be used correctly within an error handler")
  public void untilSuccessfulInErrorHandler() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulInErrorHandler").run();
    assertThat(CustomMP.getCount(), is(1));
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
  }

  private List<Object> ponderUntilMessageCountReceivedByTargetMessageProcessor(final int expectedCount)
      throws InterruptedException {
    return ponderUntilMessageCountReceived(expectedCount, targetMessageProcessor);
  }

  private List<Object> ponderUntilMessageCountReceived(final int expectedCount, final FunctionalTestProcessor ftc)
      throws InterruptedException {
    final List<Object> results = new ArrayList<>();

    new PollingProber(RECEIVE_TIMEOUT, 200).check(new JUnitLambdaProbe(() -> {
      assertThat(ftc.getReceivedMessagesCount(), greaterThanOrEqualTo(expectedCount));
      return true;
    }));

    for (int i = 0; i < ftc.getReceivedMessagesCount(); i++) {
      results.add(ftc.getReceivedMessage(1 + i).getMessage().getPayload().getValue());
    }
    return results;
  }

  private void ponderUntilMessageCountReceivedByCustomMP(final int expectedCount) throws InterruptedException {
    new PollingProber(RECEIVE_TIMEOUT, 200).check(new JUnitLambdaProbe(() -> {
      assertThat(CustomMP.getCount(), greaterThanOrEqualTo(expectedCount));
      return true;
    }));
  }

  public static class CustomMP implements Processor {

    private static List<CoreEvent> processedEvents = synchronizedList(new ArrayList<>());

    public static void clearCount() {
      processedEvents.clear();
    }

    public static int getCount() {
      return processedEvents.size();
    }

    public static List<CoreEvent> getProcessedEvents() {
      return processedEvents;
    }

    @Override
    public CoreEvent process(final CoreEvent event) throws MuleException {
      processedEvents.add(event);
      return event;
    }
  }

  public static class WaitMeasure implements Processor {

    public static long totalWait;
    private long firstAttemptTime = 0;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (firstAttemptTime == 0) {
        firstAttemptTime = currentTimeMillis();
      } else {
        totalWait = currentTimeMillis() - firstAttemptTime;
      }

      return event;
    }
  }
}
