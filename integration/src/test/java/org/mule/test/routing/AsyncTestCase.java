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
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
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
    terminationLatch.await();
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

    assertThat(afterAsyncMessage.getMessage().getPayload().getValue().toString(), startsWith("[MuleRuntime].cpuIntensive."));
  }

  @Test
  @Description("Assert that if no maxConcurrency is configured for an async, the value from the flow is inherited")
  public void withFlowMaxConcurrency() throws Exception {
    testAsyncMaxConcurrency("with-flow-max-concurrency");
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
