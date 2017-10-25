/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class TriggerScheduleTestCase extends AbstractSchedulerTestCase {

  public static final String SCHEDULER_NAME = "testScheduler";

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
    l1.await(RECEIVE_TIMEOUT, MILLISECONDS);

    withContextClassLoader(ConfigurationComponentLocator.class.getClassLoader(),
                           () -> locator.find(builderFromStringRepresentation("triggerMeFlow/source").build())
                               .map(source -> (SchedulerMessageSource) source).ifPresent(SchedulerMessageSource::trigger));

    l2.await(RECEIVE_TIMEOUT, MILLISECONDS);
  }

  public static class Foo implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      assertThat(currentThread().getContextClassLoader(), sameInstance(TriggerScheduleTestCase.class.getClassLoader()));

      l1.countDown();
      l2.countDown();
    }
  }


}
