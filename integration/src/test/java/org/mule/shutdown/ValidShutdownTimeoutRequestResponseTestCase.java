/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
@RunnerDelegateTo(FlakinessDetectorTestRunner.class)
public class ValidShutdownTimeoutRequestResponseTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "" + RECEIVE_TIMEOUT);

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @After
  public void disposeHttpClient() {
    httpClient.stop();
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/shutdown-timeout-request-response-config.xml";
  }

  @Test
  public void testScriptComponent() throws Throwable {
    doShutDownTest("scriptComponentResponse", "http://localhost:" + httpPort.getNumber() + "/scriptComponent");
  }

  @Test
  public void testSetPayload() throws Throwable {
    doShutDownTest("setPayloadResponse", "http://localhost:" + httpPort.getNumber() + "/setPayload");
  }

  @Test
  public void testSetPayloadChoice() throws Throwable {
    doShutDownTest("setPayloadResponse", "http://localhost:" + httpPort.getNumber() + "/setPayloadChoice");
  }

  @Test
  public void testSetPayloadTx() throws Throwable {
    doShutDownTest("setPayloadResponse", "http://localhost:" + httpPort.getNumber() + "/setPayloadTx");
  }

  private void doShutDownTest(final String payload, final String url) throws Throwable {
    final Future<?> requestTask = executor.submit(() -> {
      try {
        new PollingProber().check(new JUnitLambdaProbe(() ->{
          HttpRequest request =
              HttpRequest.builder().uri(url).method(GET).entity(new ByteArrayHttpEntity(payload.getBytes())).build();
          final HttpResponse response =
              httpClient.send(request, 1000, false, null);
          assertThat(IOUtils.toString(response.getEntity().getContent()), is(payload));
          return true;
        }, "Was not able to process message "));

      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();
    contextStopLatch.release();

    muleContext.stop();

    try {
      requestTask.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
