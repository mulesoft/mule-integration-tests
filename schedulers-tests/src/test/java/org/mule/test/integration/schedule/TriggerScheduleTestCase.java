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
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.concurrent.CountDownLatch;

import io.qameta.allure.Feature;

import org.junit.ClassRule;
import org.junit.Test;

@Feature(SCHEDULER)
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
    assertThat(l1.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));

    withContextClassLoader(ConfigurationComponentLocator.class.getClassLoader(),
                           () -> locator.find(builderFromStringRepresentation("triggerMeFlow/source").build())
                               .map(source -> (SchedulerMessageSource) source).ifPresent(SchedulerMessageSource::trigger));

    assertThat(l2.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  @Test
  public void restartedSchedulerFlowRunsWithAppClassLoader() throws Exception {
    assertThat(l1.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));

    withContextClassLoader(ConfigurationComponentLocator.class.getClassLoader(),
                           () -> locator.find(builderFromStringRepresentation("triggerMeFlow/source").build())
                               .map(source -> (SchedulerMessageSource) source).ifPresent(sms -> {
                                 try {
                                   sms.stop();
                                   sms.start();
                                 } catch (MuleException e) {
                                   throw new MuleRuntimeException(e);
                                 }
                               }));

    assertThat(l2.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
  }

  public static Object foo(String payload) {
    assertThat(currentThread().getContextClassLoader(), sameInstance(muleContext.getExecutionClassLoader()));

    l1.countDown();
    l2.countDown();

    return payload;
  }

}
