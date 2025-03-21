/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tests.api.TestQueueManager;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class ValidShutdownTimeoutOneWayTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "" + RECEIVE_TIMEOUT);

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/shutdown-timeout-one-way-config.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  public void testStaticComponent() throws Throwable {
    doShutDownTest("staticComponentResponse", "staticComponentFlow");
  }

  @Test
  public void testScriptComponent() throws Throwable {
    doShutDownTest("scriptComponentResponse", "scriptComponentFlow");
  }

  @Test
  public void testSetPayload() throws Throwable {
    doShutDownTest("setPayloadResponse", "setPayloadFlow");
  }

  @Test
  public void testSetPayloadChoice() throws Throwable {
    doShutDownTest("setPayloadResponse", "setPayloadChoiceFlow");
  }

  @Test
  public void setPayloadThroughScatterGatherWithFlowRefs() throws Throwable {
    doShutDownTest("setPayloadResponse", "setPayloadThroughScatterGatherWithFlowRefs");
  }

  private void doShutDownTest(final String payload, final String flowName) throws Throwable {
    final Future<?> requestTask = executor.submit(() -> {
      try {
        flowRunner(flowName).withPayload(payload).dispatch();
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();
    contextStopLatch.release();

    Message response = queueManager.read("response", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    if (response.getPayload().getDataType() instanceof MapDataType) {
      Map<String, Message> values = (Map) response.getPayload().getValue();
      values.entrySet().forEach(
                                value -> {
                                  try {
                                    assertThat("Was not able to process message", getPayloadAsString(value.getValue()),
                                               is(payload));
                                  } catch (Exception e) {
                                    fail("Was not able to process message");
                                  }
                                });
    } else {
      assertThat("Was not able to process message", getPayloadAsString(response), is(payload));
    }

    muleContext.stop();

    try {
      requestTask.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
