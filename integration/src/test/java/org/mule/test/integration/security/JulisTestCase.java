/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;

public class JulisTestCase extends AbstractIntegrationTestCase {

  CloseableHttpClient httpClient;
  CloseableHttpResponse httpResponse;

  @Rule
  public DynamicPort listenPort1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/security/julisTest.xml";
  }

  @Before
  public void before() {}

  @After
  public void tearDown() {
    closeQuietly(httpResponse);
    closeQuietly(httpClient);
  }

  @Test
  public void test() throws Exception {
    HttpPost httpPost = new HttpPost(format("http://localhost:%s/basic", listenPort1.getNumber()));
    httpClient = HttpClients.custom().build();
    httpResponse = httpClient.execute(httpPost);

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }
}
