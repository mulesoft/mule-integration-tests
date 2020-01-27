/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class ExpiredShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-one-way-config.xml";
  }

  @Test
  public void testStaticComponent() throws Throwable {
    doShutDownTest("staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Throwable {
    doShutDownTest("scriptComponentFlow");
  }

  @Test
  public void testSetPayload() throws Throwable {
    doShutDownTest("setPayloadFlow");
  }

  @Test
  public void testSetPayloadChoice() throws Throwable {
    doShutDownTest("setPayloadChoiceFlow");
  }

  private void doShutDownTest(final String flowName) throws Throwable {
    final TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    final Future<?> requestTask = executor.submit(() -> {
      try {
        flowRunner(flowName).withPayload(TEST_MESSAGE).dispatch();
        assertThat("Was able to process message ", queueHandler.read("response", RECEIVE_TIMEOUT), is(nullValue()));
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
