/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static java.lang.String.format;
import static org.mule.extension.http.api.error.HttpError.RESPONSE_VALIDATION;
import static org.mule.extension.http.internal.listener.HttpListener.HTTP_NAMESPACE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.service.http.api.HttpConstants.HttpStatus.EXPECTATION_FAILED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.FORBIDDEN;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.service.http.api.HttpConstants.HttpStatus.METHOD_NOT_ALLOWED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_ACCEPTABLE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_FOUND;
import static org.mule.service.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;
import static org.mule.service.http.api.HttpConstants.HttpStatus.TOO_MANY_REQUESTS;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERRORS;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.rules.ExpectedError;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.service.http.api.HttpConstants.HttpStatus;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories({ERROR_HANDLING, ERRORS})
public class HttpRequestErrorHandlingTestCase extends AbstractHttpRequestTestCase {

  @Rule
  public DynamicPort unusedPort = new DynamicPort("unusedPort");
  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  private int serverStatus = 200;
  private String serverContentType = "text/html";
  private boolean timeout = false;
  private Latch done = new Latch();

  @Override
  protected String getConfigFile() {
    return "http-request-errors-config.xml";
  }

  @Test
  public void badRequest() throws Exception {
    verifyErrorWhenReceiving(BAD_REQUEST, ": bad request");
  }

  @Test
  public void unauthorised() throws Exception {
    verifyErrorWhenReceiving(UNAUTHORIZED, ": unauthorized");
  }

  @Test
  public void forbidden() throws Exception {
    verifyErrorWhenReceiving(FORBIDDEN, ": forbidden");
  }

  @Test
  public void notFound() throws Exception {
    verifyErrorWhenReceiving(NOT_FOUND, String.format(": resource http://localhost:%s/testPath not found", httpPort.getValue()));
  }

  @Test
  public void methodNotAllowed() throws Exception {
    verifyErrorWhenReceiving(METHOD_NOT_ALLOWED, ": method GET not allowed");
  }

  @Test
  public void notAcceptable() throws Exception {
    verifyErrorWhenReceiving(NOT_ACCEPTABLE, ": not acceptable");
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    verifyErrorWhenReceiving(UNSUPPORTED_MEDIA_TYPE, ": media type application/xml not supported");
  }

  @Test
  public void tooManyRequest() throws Exception {
    verifyErrorWhenReceiving(TOO_MANY_REQUESTS, ": too many requests");
  }

  @Test
  public void serverError() throws Exception {
    verifyErrorWhenReceiving(INTERNAL_SERVER_ERROR, ": internal server error");
  }

  @Test
  public void serverUnavailable() throws Exception {
    verifyErrorWhenReceiving(SERVICE_UNAVAILABLE, ": service unavailable");
  }

  @Test
  public void notMappedStatus() throws Exception {
    verifyErrorWhenReceiving(EXPECTATION_FAILED, "417 not understood", RESPONSE_VALIDATION.name(),
                             getErrorMessage(EXPECTATION_FAILED, ""));
  }

  @Test
  public void timeout() throws Exception {
    timeout = true;
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    done.release();
    assertThat(result.getMessage().getPayload().getValue(), is("Timeout exceeded timeout"));
  }

  @Test
  public void connectivity() throws Exception {
    Event result = getFlowRunner("handled", unusedPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is("Connection refused connectivity"));
  }

  @Test
  public void parsing() throws Exception {
    serverContentType = "multipart/form-data; boundary=\"sdgksdg\"";
    InputStream mockStream = mock(InputStream.class);
    when(mockStream.read()).thenThrow(IOException.class);
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is("Unable to process multipart response parsing"));
  }

  private String getErrorMessage(HttpStatus status, String customMessage) {
    return String.format("Response code %s mapped as failure%s.", status.getStatusCode(), customMessage);
  }

  void verifyErrorWhenReceiving(HttpStatus status, String expectedMessage) throws Exception {
    verifyErrorWhenReceiving(status, format("%s %s", status.getStatusCode(), status.getReasonPhrase()), status.name(),
                             getErrorMessage(status, expectedMessage));
  }

  void verifyErrorWhenReceiving(HttpStatus status, Object expectedResult, String expectedError, String expectedMessage)
      throws Exception {
    serverStatus = status.getStatusCode();
    // Hit flow with error handler
    Event result = getFlowRunner("handled", httpPort.getNumber()).run();
    assertThat(result.getMessage().getPayload().getValue(), is(expectedResult));
    // Hit flow that will throw back the error
    this.expectedError.expectErrorType(HTTP_NAMESPACE.toUpperCase(), expectedError);
    this.expectedError.expectMessage(is(expectedMessage));
    getFlowRunner("unhandled", httpPort.getNumber()).run();
  }

  private FlowRunner getFlowRunner(String flowName, int port) {
    return flowRunner(flowName).withVariable("port", port);
  }

  @Override
  protected void writeResponse(HttpServletResponse response) throws IOException {
    if (timeout) {
      try {
        done.await();
      } catch (InterruptedException e) {
        // Do nothing
      }
    }
    response.setContentType(serverContentType);
    response.setStatus(serverStatus);
    response.getWriter().print(DEFAULT_RESPONSE);
  }

}
