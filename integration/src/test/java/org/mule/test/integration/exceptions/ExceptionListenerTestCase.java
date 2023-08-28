/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.api.notification.ErrorHandlerNotificationListener;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class ExceptionListenerTestCase extends AbstractIntegrationTestCase {

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;

  @Inject
  private TestQueueManager queueManager;

  private ErrorHandlerNotification exceptionStrategyStartNotification;
  private ErrorHandlerNotification exceptionStrategyEndNotification;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-listener-config-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    exceptionStrategyStartNotification = null;
    exceptionStrategyEndNotification = null;
    muleContext.getNotificationManager().addListener(new ErrorHandlerNotificationListener<ErrorHandlerNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(ErrorHandlerNotification notification) {
        if (new IntegerAction(PROCESS_START).equals(notification.getAction())) {
          exceptionStrategyStartNotification = notification;
        } else if (new IntegerAction(PROCESS_END).equals(notification.getAction())) {
          exceptionStrategyEndNotification = notification;
        }
      }
    });
  }

  @Test
  public void testExceptionStrategyFromComponent() throws Exception {
    assertQueueIsEmpty("error.queue");

    flowRunner("mycomponent").withPayload("test").dispatch();

    assertQueueIsEmpty("component.out");

    Message message = queueManager.read("error.queue", 2000, MILLISECONDS).getMessage();
    assertNotNull(message);
    Object payload = message.getPayload().getValue();
    assertThat(payload, is("test"));

    assertNotificationsArrived();
    assertNotificationsHaveMatchingResourceIds();
  }

  private void assertNotificationsHaveMatchingResourceIds() {
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is(not(nullValue())));
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is("mycomponent"));
    assertThat(exceptionStrategyStartNotification.getResourceIdentifier(),
               is(exceptionStrategyEndNotification.getResourceIdentifier()));
  }

  private void assertNotificationsArrived() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(exceptionStrategyStartNotification, is(not(nullValue())));
        assertThat(exceptionStrategyEndNotification, is(not(nullValue())));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Did not get exception strategy notifications";
      }
    });
  }

  private void assertQueueIsEmpty(String queueName) throws MuleException {
    assertThat(queueManager.read(queueName, 2000, MILLISECONDS), is(nullValue()));
  }
}
