/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
@RunnerDelegateTo(Parameterized.class)
public class ExpiredShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Parameters(name = "tx: {0}")
  public static List<Boolean> parameters() {
    return asList(false, true);
  }

  private final boolean runWithtinTx;

  public ExpiredShutdownTimeoutOneWayTestCase(boolean runWithtinTx) {
    this.runWithtinTx = runWithtinTx;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/shutdown-timeout-one-way-config.xml";
  }

  public void testStaticComponent() throws Throwable {
    doShutDownTest("staticComponentFlow");
  }

  public void testScriptComponent() throws Throwable {
    doShutDownTest("scriptComponentFlow");
  }

  public void testSetPayload() throws Throwable {
    doShutDownTest("setPayloadFlow");
  }

  public void testSetPayloadChoice() throws Throwable {
    doShutDownTest("setPayloadChoiceFlow");
  }

  @Test
  public void testSetPayloadScatterGather() throws Throwable {
    doShutDownTest("setPayloadScatterGather");
  }

  private void doShutDownTest(final String flowName) throws Throwable {
    final Future<?> requestTask = executor.submit(() -> {
      try {
        FlowRunner runner = flowRunner(flowName).withPayload(TEST_MESSAGE);
        if (runWithtinTx) {
          Transaction transaction = mock(Transaction.class);
          runner = runner.transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction));
        }
        runner.dispatch();
        assertThat("Was able to process message ", queueManager.read("response", RECEIVE_TIMEOUT, MILLISECONDS), is(nullValue()));
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();

    muleContext.stop();
    contextStopLatch.release();

    try {
      requestTask.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
