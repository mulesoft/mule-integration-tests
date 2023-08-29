/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.http.functional.AbstractHttpTestCase;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerHostNameTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-host-name-config.xml";
  }

  @Test
  public void routeToTheRightListener() throws Exception {
    final String url = String.format("http://localhost:%s/", listenPort.getNumber());
    final Response response = Request.Get(url).connectTimeout(RECEIVE_TIMEOUT).execute();
    assertThat(response.returnContent().asString(), is("ok"));
  }

}
