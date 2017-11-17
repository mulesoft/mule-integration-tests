/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PollScheduleNotificationTestCase extends AbstractSchedulerTestCase {

  private Prober prober = new PollingProber(RECEIVE_TIMEOUT, 100l);
  private MyListener listener = new MyListener();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/poll-notifications-config.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {
      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getNotificationManager().addListener(listener);
      }

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
        // Nothing to do
      }
    });
  }


  @Test
  public void validateNotificationsAreSent() throws Exception {
    prober.check(new JUnitLambdaProbe(() -> {
      assertThat(listener.getNotifications(), hasSize(greaterThan(1)));
      assertThat(listener.getNotifications().get(0).getLocationUri(), is("pollfoo/scheduler"));

      return true;
    }));
  }

  class MyListener implements ConnectorMessageNotificationListener<ConnectorMessageNotification> {

    List<ConnectorMessageNotification> notifications = new ArrayList<>();

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(ConnectorMessageNotification notification) {
      notifications.add(notification);
    }

    public List<ConnectorMessageNotification> getNotifications() {
      return notifications;
    }
  }
}
