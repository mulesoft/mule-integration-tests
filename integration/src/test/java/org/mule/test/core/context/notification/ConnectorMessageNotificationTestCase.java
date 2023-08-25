/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ConnectorMessageNotificationTestCase extends AbstractNotificationTestCase {

  private static final String FLOW_ID = "testFlow";

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/connector-message-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    HttpRequest request =
        HttpRequest.builder().uri(String.format("http://localhost:%s/path", port.getNumber())).method(POST).build();

    httpClient.send(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).build());

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        .parallel(new Node(ConnectorMessageNotification.class, new IntegerAction(MESSAGE_RECEIVED), FLOW_ID))
        .parallel(new Node(ConnectorMessageNotification.class, new IntegerAction(MESSAGE_RESPONSE), FLOW_ID));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    // Nothing to validate
  }
}
