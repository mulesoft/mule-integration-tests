/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

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


  public static class EventReceptionsCounter extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      eventsReceivedCount.incrementAndGet();
    }
  }

  public interface SchedulerNotificationListener extends NotificationListener<ConnectorMessageNotification> {
  }

}
