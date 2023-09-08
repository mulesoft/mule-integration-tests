/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.streaming;

import static java.lang.String.format;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;

public class LogStreamDataRepeatedlyTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/streaming/log-repeatable-stream-config.xml";
  }

  @Test
  public void logsRepeatableStreamTwice() throws Exception {
    HttpPost httpPost = new HttpPost(format("http://localhost:%s/", listenPort.getNumber()));
    httpPost.setEntity(new StringEntity("{\"name\": \"tato\", \"id\": \"42\"}", APPLICATION_JSON));

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      try (CloseableHttpResponse response = client.execute(httpPost)) {
        assertThat(response.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
        assertThat(IOUtils.toString(response.getEntity().getContent()), is("OK"));
      }
    }
  }

}
