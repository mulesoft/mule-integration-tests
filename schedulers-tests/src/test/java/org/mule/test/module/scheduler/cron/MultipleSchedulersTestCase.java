/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.scheduler.cron;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(SCHEDULER)
public class MultipleSchedulersTestCase extends AbstractSchedulerTestCase {

  private static CountDownLatch firstRequest = new CountDownLatch(2);
  private static Latch stoppedFlowLatch = new Latch();
  private static int counter = 0;

  @Override
  protected String getConfigFile() {
    return "multiple-schedulers-config.xml";
  }

  @Before
  public void before() {
    counter = 0;
  }

  @Test
  public void schedulersAreNotSharedAcrossPollers() throws Exception {
    firstRequest.await(getTestTimeoutSecs(), SECONDS);

    registry.<Stoppable>lookupByName("poll1").get().stop();

    stoppedFlowLatch.countDown();

    new PollingProber(RECEIVE_TIMEOUT, 100)
        .check(new JUnitLambdaProbe(() -> counter == 2, () -> "Poll2 was not executed after stopping Poll1 flow"));

  }

  public static class SynchronizedPollExecutionCounter extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      if ("poll2".equals(event.getMessage().getPayload().getValue())) {
        counter++;
      }

      firstRequest.countDown();
      stoppedFlowLatch.await();
    }
  }
}
