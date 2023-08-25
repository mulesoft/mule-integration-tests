/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.shutdown;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tests.api.TestQueueManager;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

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
  @Ignore("MULE-18879")
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
