/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.concurrent.CountDownLatch;

import org.junit.ClassRule;
import org.junit.Test;

public class TriggerScheduleTestCase extends AbstractSchedulerTestCase {

  @ClassRule
  public static SystemProperty millis = new SystemProperty("frequency.millis", "1000");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/trigger-schedule-config.xml";
  }

  private static final CountDownLatch l1 = new CountDownLatch(1);
  private static final CountDownLatch l2 = new CountDownLatch(2);

  @Test
  public void triggeredFlowRunsWithAppClassLoader() throws Exception {
    assertThat(l1.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));

    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(ConfigurationComponentLocator.class.getClassLoader());
    try {
      locator.find(builderFromStringRepresentation("triggerMeFlow/source").build())
          .map(source -> (SchedulerMessageSource) source).ifPresent(SchedulerMessageSource::trigger);
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }

    assertThat(l2.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void restartedSchedulerFlowRunsWithAppClassLoader() throws Exception {
    assertThat(l1.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));

    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(ConfigurationComponentLocator.class.getClassLoader());
    try {
      locator.find(builderFromStringRepresentation("triggerMeFlow/source").build())
          .map(source -> (SchedulerMessageSource) source).ifPresent(sms -> {
            try {
              sms.stop();
              sms.start();
            } catch (MuleException e) {
              throw new MuleRuntimeException(e);
            }
          });
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }

    assertThat(l2.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  public static class Foo extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      assertThat(currentThread().getContextClassLoader(), sameInstance(muleContext.getExecutionClassLoader()));

      l1.countDown();
      l2.countDown();
    }
  }


}
