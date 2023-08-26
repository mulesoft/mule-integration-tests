/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_FLOW_TRACE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;
import static org.mule.test.allure.AllureConstants.TransactionFeature.LocalStory.LOCAL_TRANSACTION;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(ASYNC)
public class AsyncTestCase extends AbstractIntegrationTestCase {

  private static final int MAX_CONCURRENCY = 2;

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort port = new DynamicPort("http.port");

  @Rule
  public SystemProperty maxConcurrency = new SystemProperty("maxConcurrency", "" + MAX_CONCURRENCY);

  @Rule
  // TODO MULE-17752: Remove this to re-enable flowTrace for this test case
  public SystemProperty disableFlowStack = new SystemProperty(MULE_FLOW_TRACE, "false");


  private CountDownLatch terminationLatch;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/routing/async-test.xml";
  }

  @After
  public void after() throws InterruptedException {
    // Forces to wait till all contexts are finished.
    if (terminationLatch != null) {
      terminationLatch.await();
    }
  }

  @Test
  @Description("Assert that components in an async run in the correct thread according to the flow's PS")
  public void psThreadingPropagated() throws Exception {
    terminationLatch = new CountDownLatch(1);
    FlowRunner runner = flowRunner("ps-threading-propagated");
    ((BaseEventContext) (runner.buildEvent().getContext())).onTerminated((e, t) -> terminationLatch.countDown());
    runner.run();

    CoreEvent afterAsyncMessage = queueManager.read("asyncFinished", 1000, MILLISECONDS);
    assertThat(afterAsyncMessage, not(nullValue()));

    assertThat(afterAsyncMessage.getMessage().getPayload().getValue().toString(), startsWith("[MuleRuntime].uber."));
  }

  @Test
  @Story(BACKPRESSURE)
  @Description("Assert that async maxConcurrency is honored")
  public void withMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-max-concurrency");
  }

  @Test
  @Story(BACKPRESSURE)
  @Description("Assert that even if async is full, the calling flow continues executing")
  public void withMaxConcurrencyAsyncDispatched() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("with-max-concurrency", latch);

    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      assertThat("" + i, queueManager.read("asyncDispatched", 1000, MILLISECONDS), not(nullValue()));
    }
    latch.countDown();
  }

  @Test
  @Story(BACKPRESSURE)
  @Description("Assert that if no maxConcurrency is configured for an async, the value from the flow is inherited")
  public void withFlowMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-flow-max-concurrency");
  }

  @Test
  @Story(BACKPRESSURE)
  @Description("Assert that if both flow and async have maxConcurrency, they are independent")
  public void withLowerFlowMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-lower-flow-max-concurrency");
  }

  @Test
  @Story(LOCAL_TRANSACTION)
  @Description("Assert that async blocks run outside of the transaction from the caller flow")
  public void withSourceTx() throws Exception {
    terminationLatch = new CountDownLatch(0);

    final Startable withSourceTx =
        (Startable) (locator.find(Location.builderFromStringRepresentation("with-source-tx").build())).get();
    withSourceTx.start();

    assertThat(queueManager.read("asyncDispatched", RECEIVE_TIMEOUT, MILLISECONDS), not(nullValue()));
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
  }

  @Test
  @Story(LOCAL_TRANSACTION)
  @Description("Assert that async blocks run outside of the transaction from the `try` in the caller flow")
  public void withTryTx() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("with-try-tx", latch);

    assertThat(queueManager.read("asyncDispatched", 1000, MILLISECONDS), not(nullValue()));
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
    latch.countDown();
  }

  @Test
  @Story(LOCAL_TRANSACTION)
  @Description("Assert that txs within async blocks are honored")
  public void txWithinAsync() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("tx-within-async", latch);

    assertThat(queueManager.read("asyncDispatched", 1000, MILLISECONDS), not(nullValue()));
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
    latch.countDown();
  }

  @Test
  @Description("Assert that a combination of sub-flow, async, and try works as expected")
  public void tryNoTxWithinAsyncSubFlow() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("tryNoTx-within-async-subFlow", latch);

    assertThat(queueManager.read("asyncDispatched", 1000, MILLISECONDS), not(nullValue()));
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
    latch.countDown();
  }

  @Inject
  @Named("with-max-concurrency")
  private FlowConstruct withMaxConcurrency;

  @Test
  @Issue("MULE-17048")
  @Story(GRACEFUL_SHUTDOWN_STORY)
  public void flowStoppedWhileAsyncInFlight() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    flowRunner("with-max-concurrency").withPayload("").withVariable("latch", latch).run();

    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
    stopIfNeeded(withMaxConcurrency);

    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), nullValue());

    // Restart so it can be stopped again when the test ends
    startIfNeeded(withMaxConcurrency);
  }

  @Test
  @Issue("MULE-18304")
  @Description("Verify that operations inner fluxes are not terminated when within async/sub-flow combination.")
  public void asyncFlowWithSdkOperation() throws Exception {
    flowRunner("asyncFlowWithSdkOperation").run();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));

    flowRunner("asyncFlowWithSdkOperation").run();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));
  }

  @Test
  @Issue("MULE-18304")
  @Description("Verify that operations inner fluxes are not terminated when within error-handler/async/sub-flow combination.")
  public void asyncFlowWithSdkOperationInErrorHandler() throws Exception {
    flowRunner("asyncFlowWithSdkOperationInErrorHandler").runExpectingException();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));

    flowRunner("asyncFlowWithSdkOperationInErrorHandler").runExpectingException();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));
  }

  @Test
  @Issue("MULE-19091")
  @Description("Verify that operations inner fluxes are not terminated when within error-handler/async/sub-flow combination.")
  public void asyncFlowWithSdkOperationInRefErrorHandler() throws Exception {
    flowRunner("asyncFlowWithSdkOperationInRefErrorHandler").runExpectingException();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));

    flowRunner("asyncFlowWithSdkOperationInRefErrorHandler").runExpectingException();
    assertThat(queueManager.read("asyncFinished", 1000, MILLISECONDS), not(nullValue()));
  }

  private void testAsyncMaxConcurrency(String flowName) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows(flowName, latch);

    for (int i = 0; i < MAX_CONCURRENCY; ++i) {
      assertThat("" + i, queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
    }
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), nullValue());

    latch.countDown();
    assertThat(queueManager.read("asyncRunning", 1000, MILLISECONDS), not(nullValue()));
  }

  private void runFlows(String flowName, CountDownLatch latch) throws Exception {
    terminationLatch = new CountDownLatch(MAX_CONCURRENCY + 1);
    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      FlowRunner runner = flowRunner(flowName).withPayload(i).withVariable("latch", latch);
      ((BaseEventContext) (runner.buildEvent().getContext())).onTerminated((e, t) -> terminationLatch.countDown());
      runner.run();
    }
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
