/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.http.client;

import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

@Feature(HTTP_SERVICE)
@Issue("W-17370109")
public class HttpClientResponseStreamingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "http-requester-response-streaming.xml";
  }

  @Test
  public void whenClientSeesAnErrorAfterThePayloadWasCommitted_thenTheErrorIsSeenByThePayloadConsumer() throws Exception {
    TestServerThread server = new TestServerThread(httpPort.getNumber());
    server.start();

    MessagingException exception = assertThrows(MessagingException.class, () -> runFlow("theFlow"));
    assertThat(exception,
               hasCause(allOf(instanceOf(ExpressionRuntimeException.class), hasMessage(containsString("Remotely closed")))));

    server.join();
  }

  /**
   * Accepts only one connection, and handles it by reading all the incoming data, and responding a partial response (a chunked
   * response without the trailing zero-length chunk). After the partial response, it closes the connection. An HTTP Client that
   * sends a request to this server should fail to read the response.
   */
  private static class TestServerThread extends Thread {

    private final int port;

    private TestServerThread(int port) {
      this.port = port;
    }

    private Socket acceptOneConnection(int port) throws IOException {
      try (ServerSocket passiveSocket = new ServerSocket(port)) {
        return passiveSocket.accept();
      }
    }

    @Override
    public void run() {
      try (Socket peerSocket = acceptOneConnection(port)) {
        readUntilDoubleLineReturn(peerSocket);

        OutputStream outputStream = peerSocket.getOutputStream();
        outputStream.write(("""
            HTTP/1.1 200 OK
            Content-Type: text/plain; charset=iso-8859-1
            Transfer-encoding: chunked

            1
            A
            """).getBytes());
        outputStream.flush();
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void readUntilDoubleLineReturn(Socket peerSocket) throws IOException {
      InputStream inputStream = peerSocket.getInputStream();

      StringBuilder sb = new StringBuilder();
      boolean found = false;
      boolean endOfStream = false;
      while (!found && !endOfStream) {
        byte[] buffer = new byte[1024];
        int read = inputStream.read(buffer);
        if (read == -1) {
          endOfStream = true;
        } else {
          sb.append(new String(buffer, 0, read));
          String current = sb.toString();
          if (current.contains("\r\n\r\n") || current.contains("\n\n")) {
            found = true;
          }
        }
      }
    }
  }
}
