/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
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

  @Rule
  public SystemProperty maxConcurrency = new SystemProperty("maxConcurrency", "" + MAX_CONCURRENCY);

  private TestConnectorQueueHandler queueHandler;

  private CountDownLatch terminationLatch;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/routing/async-test.xml";
  }

  @Before
  public void before() {
    queueHandler = new TestConnectorQueueHandler(registry);
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

    CoreEvent afterAsyncMessage = queueHandler.read("asyncFinished", 1000);
    assertThat(afterAsyncMessage, not(nullValue()));

    assertThat(afterAsyncMessage.getMessage().getPayload().getValue().toString(), startsWith("[MuleRuntime].uber."));
  }

  @Test
  @Description("Assert that async maxConcurrency is honored")
  public void withMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-max-concurrency");
  }

  @Test
  @Description("Assert that even if async is full, the calling flow continues executing")
  public void withMaxConcurrencyAsyncDispatched() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("with-max-concurrency", latch);

    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      assertThat("" + i, queueHandler.read("asyncDispatched", 1000), not(nullValue()));
    }
    latch.countDown();
  }

  @Test
  @Description("Assert that if no maxConcurrency is configured for an async, the value from the flow is inherited")
  public void withFlowMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-flow-max-concurrency");
  }

  @Test
  @Description("Assert that if both flow and async have maxConcurrency, they are independent")
  public void withLowerFlowMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-lower-flow-max-concurrency");
  }

  @Test
  @Description("Assert that asyncs in a sub-flow don't use up the maxConcurrency of the caller flow")
  public void withinSubflowDoesntUseFlowMaxConcurrency() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("within-subflow-doesnt-use-flow-max-concurrency", latch);

    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      assertThat("" + i, queueHandler.read("asyncRunning", 1000), not(nullValue()));
    }
    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      assertThat("" + i, queueHandler.read("asyncDispatched", 1000), not(nullValue()));
    }
    latch.countDown();
  }

  @Test
  @Description("Assert that async blocks run outside of the transaction from the caller flow")
  public void withSourceTx() throws Exception {
    terminationLatch = new CountDownLatch(0);

    final Startable withSourceTx =
        (Startable) (locator.find(Location.builderFromStringRepresentation("with-source-tx").build())).get();
    withSourceTx.start();

    assertThat(queueHandler.read("asyncDispatched", RECEIVE_TIMEOUT), not(nullValue()));
    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
  }

  @Test
  @Description("Assert that async blocks run outside of the transaction from the `try` in the caller flow")
  public void withTryTx() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("with-try-tx", latch);

    assertThat(queueHandler.read("asyncDispatched", 1000), not(nullValue()));
    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
    latch.countDown();
  }

  @Test
  @Description("Assert that txs within async blocks are honored")
  public void txWithinAsync() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("tx-within-async", latch);

    assertThat(queueHandler.read("asyncDispatched", 1000), not(nullValue()));
    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
    latch.countDown();
  }

  @Test
  @Description("Assert that a combination of sub-flow, async, and try works as expected")
  public void tryNoTxWithinAsyncSubFlow() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows("tryNoTx-within-async-subFlow", latch);

    assertThat(queueHandler.read("asyncDispatched", 1000), not(nullValue()));
    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
    latch.countDown();
  }

  @Inject
  @Named("with-max-concurrency")
  private FlowConstruct withMaxConcurrency;

  @Test
  @Issue("MULE-17048")
  public void flowStoppedWhileAsyncInFlight() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    flowRunner("with-max-concurrency").withPayload("").withVariable("latch", latch).run();

    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
    stopIfNeeded(withMaxConcurrency);

    assertThat(queueHandler.read("asyncFinished", 1000), nullValue());

    // Restart so it can be stopped again when the test ends
    startIfNeeded(withMaxConcurrency);
  }

  private void testAsyncMaxConcurrency(String flowName) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    runFlows(flowName, latch);

    for (int i = 0; i < MAX_CONCURRENCY; ++i) {
      assertThat("" + i, queueHandler.read("asyncRunning", 1000), not(nullValue()));
    }
    assertThat(queueHandler.read("asyncRunning", 1000), nullValue());

    latch.countDown();
    assertThat(queueHandler.read("asyncRunning", 1000), not(nullValue()));
  }

  private void runFlows(String flowName, CountDownLatch latch) throws Exception {
    terminationLatch = new CountDownLatch(MAX_CONCURRENCY + 1);
    for (int i = 0; i < MAX_CONCURRENCY + 1; ++i) {
      FlowRunner runner = flowRunner(flowName).withPayload(i).withVariable("latch", latch);
      ((BaseEventContext) (runner.buildEvent().getContext())).onTerminated((e, t) -> terminationLatch.countDown());
      runner.run();
    }
  }

}
