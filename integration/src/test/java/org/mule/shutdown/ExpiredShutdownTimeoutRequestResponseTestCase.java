/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

public class ExpiredShutdownTimeoutRequestResponseTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "100");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-request-response-config.xml";
  }

  @Test
  public void testScriptComponent() throws Throwable {
    doShutDownTest("http://localhost:" + httpPort.getNumber() + "/scriptComponent");
  }

  @Test
  public void testExpressionTransformer() throws Throwable {
    doShutDownTest("http://localhost:" + httpPort.getNumber() + "/expressionTransformer");
  }

  private void doShutDownTest(final String url) throws Throwable {
    final Future<?> requestTask = executor.submit(() -> {
      try {
        new PollingProber().check(new JUnitLambdaProbe(() -> {
          HttpRequest request = HttpRequest.builder().uri(url).entity(new ByteArrayHttpEntity(TEST_MESSAGE.getBytes()))
              .method(POST).build();

          HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT * 5, false, null);

          assertThat("Was able to process message ", response.getStatusCode(), is(not(OK.getStatusCode())));
          return true;
        }, "Was not able to process message "));
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
