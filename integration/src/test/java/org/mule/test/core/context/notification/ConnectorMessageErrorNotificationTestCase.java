/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static java.lang.String.format;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ConnectorMessageErrorNotificationTestCase extends AbstractNotificationTestCase {

  private static final String FLOW_ID = "testFlow";
  private static final int TIMEOUT = 3000;

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/connector-message-error-notification-test-flow.xml";
  }

  @Test
  public void doTest() throws Exception {
    HttpRequest request =
        HttpRequest.builder().uri(format("http://localhost:%s/path", port.getNumber())).method(POST).build();

    httpClient.send(request, TIMEOUT, false, null);

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        .parallel(new Node(ConnectorMessageNotification.class, new IntegerAction(MESSAGE_RECEIVED), FLOW_ID))
        .parallel(new Node(ConnectorMessageNotification.class, new IntegerAction(MESSAGE_ERROR_RESPONSE), FLOW_ID));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    // Nothing to validate
  }
}
