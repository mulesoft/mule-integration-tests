/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FailingNotificationListenerTestCase extends AbstractIntegrationTestCase {

  private static int count = 0;
  private static final Object lock = new Object();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/failing-notification-listener-config.xml";
  }

  @Test
  public void testName() throws Exception {
    flowRunner("testFlow").withPayload(TEST_MESSAGE).dispatch();
    flowRunner("testFlow").withPayload(TEST_MESSAGE).dispatch();

    Prober prober = new PollingProber(1000, 10);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return count == 2;
      }

      @Override
      public String describeFailure() {
        return "Expected to received 2 notifications but received " + count;
      }
    });
  }

  public static class ExceptionFailingListener implements ExceptionNotificationListener {

    @Override
    public void onNotification(ExceptionNotification notification) {
      synchronized (lock) {
        count = count + 1;
      }

      throw new IllegalStateException();
    }
  }
}
