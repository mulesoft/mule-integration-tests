/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Feature;

import org.junit.Before;
import org.junit.Test;

@Feature(SCHEDULER)
public class SchedulerNotificationsTestCase extends AbstractSchedulerTestCase {

  private static AtomicInteger eventsReceivedCount = new AtomicInteger(0);
  private static AtomicInteger receivedNotifications = new AtomicInteger(0);
  private final int EXPECTED_EVENT_COUNT = 4;
  private final int TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-notifications-config.xml";
  }

  @Before
  public void setUp() {
    eventsReceivedCount.set(0);
    receivedNotifications.set(0);
  }

  @Test
  public void testMessageReceivedNotificationAreReceivedOncePerEmittedEvent() throws Exception {
    SchedulerNotificationListener notificationListener = notification -> {
      if (notification.getAction().getActionId() == MESSAGE_RECEIVED &&
          notification.getComponent().getLocation().getLocation().equals("notiflow/source")) {
        receivedNotifications.incrementAndGet();
      }
    };
    muleContext.getNotificationManager().addInterfaceToType(SchedulerNotificationListener.class,
                                                            ConnectorMessageNotification.class);
    muleContext.getNotificationManager().addListener(notificationListener);

    ((Flow) getFlowConstruct("notiflow")).start();

    check(TIMEOUT, 1000,
          () -> eventsReceivedCount.get() >= EXPECTED_EVENT_COUNT && receivedNotifications.get() >= EXPECTED_EVENT_COUNT);
  }

  public static Object countEventReception(String payload) {
    eventsReceivedCount.incrementAndGet();
    return payload;
  }

  public interface SchedulerNotificationListener extends NotificationListener<ConnectorMessageNotification> {
  }

}
