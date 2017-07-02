/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.scheduler.cron;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MultipleSchedulersTestCase extends MuleArtifactFunctionalTestCase {

  private static CountDownLatch firstRequest = new CountDownLatch(2);
  private static Latch stoppedFlowLatch = new Latch();
  private static int counter = 0;

  @Override
  protected String getConfigFile() {
    return "multiple-schedulers-config.xml";
  }

  @Test
  public void schedulersAreNotSharedAcrossPollers() throws Exception {
    firstRequest.await(getTestTimeoutSecs(), TimeUnit.SECONDS);

    Flow poll1 = (Flow) muleContext.getRegistry().lookupFlowConstruct("poll1");
    poll1.stop();

    stoppedFlowLatch.countDown();

    PollingProber pollingProber = new PollingProber(5000, 100);
    pollingProber.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return counter == 2;
      }

      @Override
      public String describeFailure() {
        return "Poll2 was not executed after stopping Poll1 flow";
      }
    });

  }

  public static class SynchronizedPollExecutionCounter implements EventCallback {

    @Override
    public void eventReceived(Event event, Object component, MuleContext muleContext) throws Exception {
      if ("poll2".equals(event.getMessage().getPayload().getValue())) {
        counter++;
      }

      firstRequest.countDown();
      stoppedFlowLatch.await();
    }
  }
}
