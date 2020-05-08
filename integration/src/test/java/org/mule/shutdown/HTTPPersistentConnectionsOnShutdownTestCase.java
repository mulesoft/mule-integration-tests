/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@Issue("MULE-18396")
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class HTTPPersistentConnectionsOnShutdownTestCase extends AbstractIntegrationTestCase {

  private static final int SMALL_TIMEOUT_MILLIS = 300;
  private static final int POLL_DELAY_MILLIS = 50;
  private static final String SLOW_PROCESSING_ENDPOINT = "/slow";
  private static final String FAST_PROCESSING_ENDPOINT = "/fast";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("listener.port");

  private ExecutorService stopMuleExecutor;

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/http-persistent-connections-on-shutdown.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Before
  public void setup() {
    stopMuleExecutor = Executors.newSingleThreadExecutor();
  }

  @After
  public void tearDown() throws MuleException, InterruptedException {
    stopMuleExecutor.awaitTermination(1000, MILLISECONDS);
    stopMuleExecutor.shutdownNow();
    if (muleContext.isStopped()) {
      muleContext.start();
    }
  }

  @Test
  public void requestInflightDuringShutdownIsRespondedIncludingConnectionCloseHeader() throws IOException {
    try (Socket slowRequestConnection = new Socket("localhost", dynamicPort.getNumber())) {
      sendRequest(slowRequestConnection, SLOW_PROCESSING_ENDPOINT);

      // Stop mule in parallel.
      stopMuleExecutor.execute(new MuleContextStopper());
      assertContextIsStopping(SMALL_TIMEOUT_MILLIS);

      // Response is ok, but connection close header is added.
      String slowRequestResponse = getResponse(slowRequestConnection);
      assertResponse(slowRequestResponse, true);
      assertThat(slowRequestResponse, containsString("Connection: close"));

      // After that, the connection is no longer usable.
      sendRequest(slowRequestConnection, FAST_PROCESSING_ENDPOINT);
      slowRequestResponse = getResponse(slowRequestConnection);
      assertResponse(slowRequestResponse, false);
    }
  }

  @Test
  public void serverIsStoppedWhenPersistentConnectionsAreClosed() throws IOException {
    try (Socket idlePersistentConnection = generateIdlePersistentConnection()) {
      // Stop mule in parallel.
      stopMuleExecutor.execute(new MuleContextStopper());
      assertContextIsStopping(SMALL_TIMEOUT_MILLIS);

      // There is a persistent connection open, so muleContext doesn't stop during the shutdown timeout.
      assertThat(muleContext.isStopped(), is(false));

      idlePersistentConnection.close();

      // When the persistent connection is closed, muleContext stops.
      assertContextHasStopped(SMALL_TIMEOUT_MILLIS);
    }
  }

  @Test
  public void onlyOneRequestIsValidUsingAnOldPersistentDuringShutdown() throws IOException {
    Socket persistentConnection1 = generateIdlePersistentConnection();
    Socket persistentConnection2 = generateIdlePersistentConnection();
    Socket persistentConnection3 = generateIdlePersistentConnection();

    // Stop mule in parallel.
    stopMuleExecutor.execute(new MuleContextStopper());
    assertContextIsStopping(SMALL_TIMEOUT_MILLIS);

    // One more request with the persistent connections is valid.
    sendRequest(persistentConnection1, FAST_PROCESSING_ENDPOINT);
    sendRequest(persistentConnection2, FAST_PROCESSING_ENDPOINT);
    String response1 = getResponse(persistentConnection1);
    String response2 = getResponse(persistentConnection2);
    assertResponse(response1, true);
    assertResponse(response2, true);

    // And contains the connection close header
    assertThat(response1, containsString("Connection: close"));
    assertThat(response2, containsString("Connection: close"));

    // Mule isn't stopped yet, since the 3rd connection is still open.
    assertThat(muleContext.isStopped(), is(false));

    // A second request with the already used connections isn't valid.
    sendRequest(persistentConnection1, FAST_PROCESSING_ENDPOINT);
    sendRequest(persistentConnection2, FAST_PROCESSING_ENDPOINT);
    assertResponse(getResponse(persistentConnection1), false);
    assertResponse(getResponse(persistentConnection2), false);

    // Mule isn't stopped yet, since the 3rd connection is still open.
    assertThat(muleContext.isStopped(), is(false));

    // If we close the other connection, mule context stops fast.
    persistentConnection3.close();
    assertContextHasStopped(SMALL_TIMEOUT_MILLIS);
  }

  private void assertContextIsStopping(long timeout) {
    new PollingProber(timeout, POLL_DELAY_MILLIS).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return muleContext.isStopping();
      }

      @Override
      public String describeFailure() {
        return "Timeout waiting for muleContext to be in stopping state";
      }
    });
  }

  private void assertContextHasStopped(long timeout) {
    new PollingProber(timeout, POLL_DELAY_MILLIS).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return muleContext.isStopped();
      }

      @Override
      public String describeFailure() {
        return "Timeout waiting for muleContext to be in stopped state";
      }
    });
  }

  private Socket generateIdlePersistentConnection() throws IOException {
    Socket socket = new Socket("localhost", dynamicPort.getNumber());
    assertThat(socket.isConnected(), is(true));

    sendRequest(socket, FAST_PROCESSING_ENDPOINT);
    assertResponse(getResponse(socket), true);

    sendRequest(socket, FAST_PROCESSING_ENDPOINT);
    assertResponse(getResponse(socket), true);

    return socket;
  }

  private void sendRequest(Socket socket, String endpoint) throws IOException {
    PrintWriter writer = new PrintWriter(socket.getOutputStream());
    writer.println(format("GET %s %s", endpoint, HTTP_1_1));
    writer.println("Host: www.example.com");
    writer.println("");
    writer.flush();
  }

  private String getResponse(Socket socket) {
    try (StringWriter writer = new StringWriter()) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String line;
      while (!isEmpty(line = reader.readLine())) {
        writer.append(line).append("\r\n");
      }
      return writer.toString();
    } catch (IOException e) {
      return null;
    }
  }

  private void assertResponse(String response, boolean shouldBeValid) {
    assertThat(!isEmpty(response), is(shouldBeValid));
    if (shouldBeValid) {
      assertThat(response, containsString("HTTP/1.1 200"));
    }
  }

  private static class MuleContextStopper implements Runnable {

    @Override
    public void run() {
      try {
        muleContext.stop();
      } catch (MuleException e) {
        e.printStackTrace();
      }
    }
  }
}
