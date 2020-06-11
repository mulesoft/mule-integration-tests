/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import io.qameta.allure.Issue;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.REQUEST_TOO_LONG;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.REQUEST_URI_TOO_LONG;

@Issue("MULE-18455")
public class HttpListenerUrlOrHeaderTooLongIssue18455TestCase extends AbstractIntegrationTestCase {

  private static final int SIZE_DELTA = 1000;


  @Rule
  public SystemProperty maxHeaderSectionSizeSystemProperty =
      new SystemProperty(SYSTEM_PROPERTY_PREFIX + "http.headerSectionSize", "10000");
  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Test
  public void failsWithAppropriateError() throws Exception {
    final Response response = Request.Get(getListenerUrl(repeat("path", 3000)))
        .execute();

    assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(REQUEST_URI_TOO_LONG.getStatusCode()));
  }

  @Test
  public void maxHeaderSizeExceeded() throws Exception {
    int queryParamSize = Integer.parseInt(maxHeaderSectionSizeSystemProperty.getValue()) + SIZE_DELTA;
    Response response = sendRequestWithQueryParam(queryParamSize);
    StatusLine statusLine = response.returnResponse().getStatusLine();
    assertThat(statusLine.getStatusCode(), is(REQUEST_TOO_LONG.getStatusCode()));
    assertThat(statusLine.getReasonPhrase(), is(REQUEST_TOO_LONG.getReasonPhrase()));
  }

  @Test
  public void maxHeaderSizeNotExceeded() throws Exception {
    int queryParamSize = Integer.parseInt(maxHeaderSectionSizeSystemProperty.getValue()) - SIZE_DELTA;
    HttpResponse response = sendRequestWithQueryParam(queryParamSize).returnResponse();
    assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/http-listener-url-header-too-long-config.xml";
  }

  private Response sendRequestWithQueryParam(int queryParamSize) throws IOException {
    String longHeaderValue = RandomStringUtils.randomAlphanumeric(queryParamSize);
    String urlWithQueryParameter = format("http://localhost:%d/", listenPort.getNumber());
    return Request.Get(urlWithQueryParameter).setHeader("header", longHeaderValue)
        .execute();
  }

  private String getListenerUrl(String path) {
    return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
  }
}
