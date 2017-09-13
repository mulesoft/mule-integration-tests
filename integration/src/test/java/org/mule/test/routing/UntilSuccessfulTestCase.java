/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;
import static org.mule.functional.api.component.InvocationCountMessageProcessor.getNumberOfInvocationsFor;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getExceptionPayload;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;
import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
public class UntilSuccessfulTestCase extends AbstractIntegrationTestCase {

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
    targetMessageProcessor = getFromFlow(muleContext, "target-mp");
  }

  @Test
  public void defaultConfiguration() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("minimal-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(1);
    assertThat(receivedPayloads, hasSize(1));
    assertThat(receivedPayloads.get(0), is(payload));
  }

  @Test
  public void fullConfigurationMP() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    final Message response = flowRunner("full-config-with-mp").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is("ACK"));

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }

    ponderUntilMessageCountReceivedByCustomMP(1);

    Throwable error = getExceptionPayload(CustomMP.getProcessedEvents().get(0).getMessage()).getException();
    assertThat(error, is(notNullValue()));
    assertThat(error.getCause(), instanceOf(RetryPolicyExhaustedException.class));
    assertThat(error.getCause().getMessage(),
               containsString("'until-successful' retries exhausted. Last exception message was: Value was expected to be true but it was false instead"));

    assertThat(error.getCause().getCause(), instanceOf(MuleRuntimeException.class));
    assertThat(error.getCause().getMessage(),
               containsString("Value was expected to be true but it was false instead"));
  }

  @Test
  public void retryOnEndpoint() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("retry-endpoint-config").withPayload(payload).run();

    final List<Object> receivedPayloads = ponderUntilMessageCountReceivedByTargetMessageProcessor(3);
    assertThat(receivedPayloads, hasSize(3));
    for (int i = 0; i <= 2; i++) {
      assertThat(receivedPayloads.get(i), is(payload));
    }
  }

  @Test
  public void executeSynchronously() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(RetryPolicyExhaustedException.class));
    expectedException.expectCause(hasCause(instanceOf(FunctionalTestException.class)));
    flowRunner("synchronous").withPayload(payload).run();
  }

  @Test
  public void executeSynchronouslyDoingRetries() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("synchronous-with-retry").withPayload(payload).runExpectingException();
    assertThat(getNumberOfInvocationsFor("untilSuccessful"), is(4));
    assertThat(getNumberOfInvocationsFor("exceptionStrategy"), is(1));
  }

  /**
   * Verifies that the synchronous wait time is consistent with that requested
   */
  @Test
  public void measureSynchronousWait() throws Exception {
    final String payload = RandomStringUtils.randomAlphanumeric(20);
    flowRunner("measureSynchronousWait").withPayload(payload).runExpectingException();
    assertThat(WaitMeasure.totalWait >= 1000, is(true));
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

    private static List<BaseEvent> processedEvents = new ArrayList<>();

    public static void clearCount() {
      processedEvents.clear();
    }

    public static int getCount() {
      return processedEvents.size();
    }

    public static List<BaseEvent> getProcessedEvents() {
      return processedEvents;
    }

    @Override
    public BaseEvent process(final BaseEvent event) throws MuleException {
      processedEvents.add(event);
      return event;
    }
  }

  public static class WaitMeasure implements Processor {

    public static long totalWait;
    private long firstAttemptTime = 0;

    @Override
    public BaseEvent process(BaseEvent event) throws MuleException {
      if (firstAttemptTime == 0) {
        firstAttemptTime = System.currentTimeMillis();
      } else {
        totalWait = System.currentTimeMillis() - firstAttemptTime;
      }

      return event;
    }
  }
}
