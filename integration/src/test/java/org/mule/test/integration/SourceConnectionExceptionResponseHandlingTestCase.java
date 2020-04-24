/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERROR_HANDLING;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(HTTP_EXTENSION)
@Story(ERROR_HANDLING)
@Issue("MULE-18343")
public class SourceConnectionExceptionResponseHandlingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/source-connection-exception-response-handling-test-case.xml";
  }

  @Test
  public void whenConnectionExceptionSourceIsNotifiedOfError() throws IOException, TimeoutException {
    HttpResponse httpResponse =
        httpClient.send(HttpRequest.builder().uri(String.format("http://localhost:%s/", httpPort.getNumber())).build());
    assertThat(httpResponse.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

}
