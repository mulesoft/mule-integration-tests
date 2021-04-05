/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

/**
 * <p>
 * Validates that a synchronous flow processing strategy implies a synchronous poll execution
 * </p>
 */
@Feature(SCHEDULER)
public class SynchronousSchedulerTestCase extends AbstractSchedulerTestCase {

  private static final int PROBE_TIMEOUT = 10000 + RECEIVE_TIMEOUT;
  private static List<String> fooSync = new ArrayList<>();
  private static List<String> fooNonSync = new ArrayList<>();

  public static CountDownLatch shutDownLatch;

  @Before
  public void before() {
    FooSync.latch = new CountDownLatch(2);
    FooNonSync.latch = new CountDownLatch(2);
    shutDownLatch = new CountDownLatch(1);

    fooSync.clear();
    fooNonSync.clear();
  }

  @Override
  protected String getConfigFile() {
    return "cron-synchronous-scheduler-config.xml";
  }

  @After
  public void after() throws MuleException {
    shutDownLatch.countDown();
    ((SchedulerMessageSource) locator.find(Location.builder().globalName("nonSynchronousPoll").addSourcePart().build()).get())
        .stop();
    ((SchedulerMessageSource) locator.find(Location.builder().globalName("synchronousPoll").addSourcePart().build()).get())
        .stop();
  }

  @Test
  public void testSynchronous() throws InterruptedException {
    assertThat(FooSync.latch.await(7000, MILLISECONDS), is(false));
    assertThat(fooSync, hasSize(1));
  }

  @Test
  public void testNonSynchronous() throws InterruptedException {
    assertThat(FooNonSync.latch.await(7000, MILLISECONDS), is(true));
    probe(PROBE_TIMEOUT, 100, () -> {
      assertThat(fooNonSync, hasSize(greaterThanOrEqualTo(2)));
      return true;
    });
  }

  public static class FooNonSync extends AbstractComponent implements EventCallback {

    public static CountDownLatch latch;

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      latch.countDown();

      synchronized (fooNonSync) {
        fooNonSync.add((String) event.getMessage().getPayload().getValue());
        try {
          shutDownLatch.await(10000, MILLISECONDS);
        } catch (InterruptedException e) {

        }
      }
    }
  }

  public static class FooSync extends AbstractComponent implements EventCallback {

    public static CountDownLatch latch;

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      latch.countDown();

      synchronized (fooSync) {
        fooSync.add((String) event.getMessage().getPayload().getValue());
        try {
          shutDownLatch.await(10000, MILLISECONDS);
        } catch (InterruptedException e) {

        }
      }
    }
  }

}
