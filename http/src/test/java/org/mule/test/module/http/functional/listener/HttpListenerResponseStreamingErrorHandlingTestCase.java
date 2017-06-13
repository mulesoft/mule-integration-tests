/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.apache.http.client.fluent.Request.Get;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.STREAMING;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories({ERROR_HANDLING, STREAMING})
public class HttpListenerResponseStreamingErrorHandlingTestCase extends AbstractHttpListenerErrorHandlingTestCase {

  final static int TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "http-listener-response-streaming-exception-strategy-config.xml";
  }

  @Test
  public void whenBuildingResponseHandlerCalledAndErrorReturned() throws Exception {
    final Response response =
        Get(getUrl("exceptionBuildingResponse")).connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse, "java.io.IOException: Some exception");
  }

  @Test
  public void whenSendingResponseHandlerNotCalledAndErrorReturned() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponse")).connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }

  @Test
  public void whenBuildingResponseFailsTwiceHandlerCalledAndErrorReturned() throws Exception {
    final Response response = Get(getUrl("exceptionBuildingResponseFailAgain")).connectTimeout(TIMEOUT)
        .socketTimeout(TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyFailed(httpResponse, "java.io.IOException: Some exception");
  }

  @Test
  public void whenSendingResponseFailsTwiceHandlerNotCalledAndErrorReturned() throws Exception {
    final Response response =
        Get(getUrl("exceptionSendingResponseFailAgain")).connectTimeout(TIMEOUT).socketTimeout(TIMEOUT).execute();

    final HttpResponse httpResponse = response.returnResponse();

    assertExceptionStrategyNotExecuted(httpResponse);
  }


}
