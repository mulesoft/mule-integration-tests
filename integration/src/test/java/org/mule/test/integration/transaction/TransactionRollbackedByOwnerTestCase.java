/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.runners.Parameterized;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mule.test.runner.RunnerDelegateTo;

@RunnerDelegateTo(Parameterized.class)
public class TransactionRollbackedByOwnerTestCase extends AbstractIntegrationTestCase {

  private static final int POLL_DELAY_MILLIS = 100;

  private List<TransactionNotification> notifications;
  private String flowName;
  private String secondActionExpected;
  private boolean throwsMessagingException;


  @Parameterized.Parameters(name = "{0}")
  public static Object[][] params() {
    return new Object[][] {
        new Object[] {"no-rollback", false, "commit"},
        new Object[] {"rollback", true, "rollback"},
        new Object[] {"no-rollback-outside-try", true, "commit"},
        new Object[] {"no-rollback-flowref", false, "commit"},
        new Object[] {"no-rollback-error-in-flow-ref", false, "commit"},
        new Object[] {"rollback-error-in-flow-ref-with-try", true, "rollback"},
        new Object[] {"no-rollback-error-in-flow-ref-with-try", false, "commit"},
        new Object[] {"no-rollback-error-in-flow-ref-with-try-join-tx", false, "commit"}
    };
  }

  public TransactionRollbackedByOwnerTestCase(String flowName, boolean throwsMessagingException, String secondNotification) {
    this.flowName = flowName;
    this.secondActionExpected = secondNotification;
    this.throwsMessagingException = throwsMessagingException;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/transaction-owner.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    notifications = new ArrayList<>();
  }

  @Test
  public void testInitializeIsCalledInInnerExceptionStrategy() throws Exception {
    final TransactionNotificationListener listener = new TransactionNotificationListener<TransactionNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(TransactionNotification notification) {
        notifications.add(notification);
      }
    };
    muleContext.getNotificationManager().addListener(listener);
    try {
      flowRunner(flowName).withPayload("message").run();
      if (throwsMessagingException) {
        fail("Should have thrown Exception from unhandled error");
      }
    } catch (Exception e) {
      if (!throwsMessagingException) {
        fail("Should have not thrown Exception from handled error");
      }
    }

    assertNotificationsArrived();
    assertCorrectNotifications();
  }

  private void assertNotificationsArrived() {
    PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLL_DELAY_MILLIS);
    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(notifications.size(), greaterThanOrEqualTo(2));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Notifications did not arrive";
      }
    });
  }

  private void assertCorrectNotifications() {
    assertThat(notifications.get(0).getActionName(), is("begin"));
    assertThat(notifications.get(1).getActionName(), is(secondActionExpected));
  }
}
