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

    @Override
    public void run() {
      try {
        ServerSocket passiveSocket = new ServerSocket(port);
        Socket peerSocket = passiveSocket.accept();
        passiveSocket.close();

        DevNullReaderThread reader = new DevNullReaderThread(peerSocket);
        reader.start();

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

        reader.join();
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Reads the input stream from the passed socket, and does nothing with the data.
   */
  private static class DevNullReaderThread extends Thread {

    private final InputStream peerIS;

    public DevNullReaderThread(Socket peer) throws IOException {
      this.peerIS = peer.getInputStream();
    }

    @Override
    public void run() {
      boolean eos = false;
      while (!eos) {
        try {
          if (peerIS.read() == -1) {
            eos = true;
          }
        } catch (IOException e) {
          System.out.println(e.getMessage());
          eos = true;
        }
      }
    }
  }
}
