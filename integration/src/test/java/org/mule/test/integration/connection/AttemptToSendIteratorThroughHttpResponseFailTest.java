/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.connection;


import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class AttemptToSendIteratorThroughHttpResponseFailTest extends LazyConnectionsTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "transformer-test-config.xml";
  }

  @Test
  public void iteratorToByteArrayTransformerThrowsException() throws Exception {
    HttpResponse response = Request.Post(getUrl("test/listener")).execute().returnResponse();
    System.out.println(IOUtils.toString(response.getEntity().getContent()));
    assertThat(response.getStatusLine().getStatusCode(), equalTo(500));
  }

  private String getUrl(String path) {
    return String.format("http://localhost:%s/%s", port.getNumber(), path);
  }

}
