/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.http;

import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.tck.probe.PollingProber.probe;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import org.mule.functional.junit4.rules.HttpServerRule;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

import jakarta.inject.Inject;

public class HttpSlowInputStreamTestCase extends AbstractIntegrationTestCase {

  // Use a large value to force the selector to become blocked reading the stream
  @Rule
  public SystemProperty timeoutToUseSelectorWhileStreamingResponseMillis =
      new SystemProperty("mule.timeoutToUseSelectorWhileStreamingResponseMillis", "15000");

  @Rule
  public SystemProperty httpListenerSelectors =
      new SystemProperty("org.mule.service.http.impl.service.server.HttpListenerConnectionManager.DEFAULT_SELECTOR_THREAD_COUNT",
                         "2");

  @Rule
  public DynamicPort portListener = new DynamicPort("portListener");

  @Rule
  public DynamicPort portRequester = new DynamicPort("portRequester");

  @Rule
  public HttpServerRule httpServerRules = new HttpServerRule("portRequester");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Inject
  private MuleAlertingSupport alertingSupport;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/http/slow-input-streams-config.xml";
  }

  @Test
  // @Ignore("Grizzly puths a thread jump, Netty has a bug and this test hangs.")
  public void slowRequesterBody() throws Exception {
    for (int i = 0; i < 3; i++) {
      flowRunner("slowRequesterBody").run();
    }

    Thread.sleep(30000);

    probe(() -> {
      assertThat(alertingSupport.alertsCountAggregation(), hasKey("HTTP_SELECTOR_THREAD_BUSY"));
      return true;
    });
  }

  @Test
  public void slowListenerResponseBody() throws IOException, TimeoutException, InterruptedException {
    HttpRequest request = HttpRequest.builder().uri(format("http://localhost:%s/%s",
                                                           portListener.getNumber(),
                                                           "test"))
        .method(GET)
        .build();
    for (int i = 0; i < 1; i++) {
      final var sent = httpClient.sendAsync(request,
                                            HttpRequestOptions.builder()
                                                .responseTimeout(50000)
                                                .build());

      // new Thread(() -> {
      // try {
      // sent.get().getEntity().getBytes();
      // } catch (IOException | InterruptedException | ExecutionException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }
      // }).start();;
    }

    Thread.sleep(10000);

    probe(() -> {
      assertThat(alertingSupport.alertsCountAggregation(), hasKey("CPU_LIGHT_SCHEDULER_BUSY"));
      return true;
    });
  }

  @Inject
  private SchedulerService schedulerService;

  @Test
  public void therlock() {
    schedulerService.cpuLightScheduler().execute(() -> {
      try {
        Thread.sleep(30000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
