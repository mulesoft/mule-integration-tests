/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.security;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;

import org.junit.Ignore;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-19629")
public class SecureHttpPollingFunctionalTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "org/mule/test/spring/security/secure-http-polling-server-flow.xml",
        "org/mule/test/spring/security/secure-http-polling-client-flow.xml"};
  }

  @Test
  public void testPollingHttpConnectorSentCredentials() throws Exception {
    final Latch latch = new Latch();
    notificationListenerRegistry.registerListener(new SecurityNotificationListener<SecurityNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(SecurityNotification notification) {
        latch.countDown();
      }
    });

    Message result = queueManager.read("toclient", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(result, not(nullValue()));
    assertThat(result.getPayload().getValue(), is("foo"));

    result = queueManager.read("toclient2", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    // This seems a little odd that we forward the exception to the outbound endpoint, but I guess users
    // can just add a filter
    assertThat(result, not(nullValue()));
    assertThat(result.getAttributes().getValue(), instanceOf(HttpResponseAttributes.class));
    assertThat(((HttpResponseAttributes) result.getAttributes().getValue()).getStatusCode(), is(UNAUTHORIZED.getStatusCode()));
    assertThat(latch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }
}
