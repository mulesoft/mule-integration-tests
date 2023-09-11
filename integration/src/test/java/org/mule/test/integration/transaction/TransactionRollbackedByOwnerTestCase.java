/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY;

import org.junit.Rule;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class TransactionRollbackedByOwnerTestCase extends AbstractIntegrationTestCase {

  private static final int POLL_DELAY_MILLIS = 100;

  private List<TransactionNotification> notifications;
  private final String flowName;
  private final String secondActionExpected;
  private final boolean throwsMessagingException;
  private final String config;

  @Rule
  public SystemProperty defaultErrorHandler =
      new SystemProperty(DEFAULT_ERROR_HANDLER_NOT_ROLLBACK_IF_NOT_CORRESPONDING_PROPERTY, "true");

  @Parameters(name = "{0} - {2}")
  public static Object[][] params() {
    return new Object[][] {
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-outside-try", true, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-flowref", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "rollback-error-in-flow-ref-with-try", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-commits", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-implicit-default-EH-executed-rollback", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-commits", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner.xml",
            "with-default-EH-executed-rollback", false, "rollback"},

        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-on-error-prop", false, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-default-on-error-prop", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-in-flow", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-flow-on-error-continue", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-subflows", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-subflows", false, "commit"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "rollback-nested-flows", true, "rollback"},
        new Object[] {"Local Error Handler", "org/mule/test/integration/transaction/transaction-owner-subflow.xml",
            "commit-nested-flows", false, "commit"},

        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback", false, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback", false, "rollback"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-outside-try", true, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-flowref", false, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref", false, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-try", true, "rollback"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try", false, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, "rollback"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false, "commit"},
        new Object[] {"Global Error Handler", "org/mule/test/integration/transaction/transaction-owner-global-err.xml",
            "no-rollback-error-in-flow-ref-with-try-join-tx", false, "commit"},

        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback", true, "rollback"},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-outside-try", true, "commit"},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-try", true, "rollback"},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "rollback-error-in-flow-ref-with-nested-try", false, "rollback"},
        new Object[] {"Default Error Handler", "org/mule/test/integration/transaction/transaction-owner-default-err.xml",
            "no-rollback-error-in-flow-ref-with-nested-try", false, "commit"}
    };
  }

  public TransactionRollbackedByOwnerTestCase(String type, String config, String flowName, boolean throwsMessagingException,
                                              String secondNotification) {
    this.flowName = flowName;
    this.secondActionExpected = secondNotification;
    this.throwsMessagingException = throwsMessagingException;
    this.config = config;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Override
  protected void doSetUp() throws Exception {
    notifications = new ArrayList<>();
  }

  @Test
  public void checkRollback() throws Exception {
    final TransactionNotificationListener listener = notification -> notifications.add((TransactionNotification) notification);
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
