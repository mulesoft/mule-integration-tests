/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.lang.Thread.sleep;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import io.qameta.allure.Story;

@Story(ERROR_HANDLING)
public class SchedulerBackpressureHandlingTestCase extends AbstractSchedulerTestCase {

  private static Latch hangLatch = new Latch();
  private static Latch latch = new Latch();
  private static volatile boolean normalized = false;
  private static AtomicInteger totalCount = new AtomicInteger(0);
  private static AtomicInteger normalizedCount = new AtomicInteger(0);

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-backpressure-config.xml";
  }

  @Test
  public void backpressureAndRecoveryWithScheduler() throws Exception {
    hangLatch.await();

    locator.find(builderFromStringRepresentation("generatesBackpressure/source").build())
        .map(source -> (SchedulerMessageSource) source)
        .ifPresent(SchedulerMessageSource::trigger);
    locator.find(builderFromStringRepresentation("generatesBackpressure/source").build())
        .map(source -> (SchedulerMessageSource) source)
        .ifPresent(SchedulerMessageSource::trigger);

    sleep(1200);

    latch.countDown();
    normalized = true;

    probe(RECEIVE_TIMEOUT, 100, () -> normalizedCount.get() == 2,
          () -> "The triggers in the middle were held back instead of rejected");
  }

  public static class HangThreadCallback implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      if (!normalized) {
        hangLatch.countDown();
        latch.await();
      }

      totalCount.incrementAndGet();
      if (normalized) {
        normalizedCount.incrementAndGet();
      }
    }
  }

}
