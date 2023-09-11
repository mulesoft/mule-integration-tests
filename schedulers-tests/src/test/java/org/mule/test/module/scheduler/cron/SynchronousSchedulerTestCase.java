/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.qameta.allure.Feature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
  public static CountDownLatch latchNonSync;
  public static CountDownLatch latchSync;

  @Before
  public void before() {
    latchNonSync = new CountDownLatch(2);
    latchSync = new CountDownLatch(2);
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
    assertThat(latchSync.await(7000, MILLISECONDS), is(false));
    assertThat(fooSync, hasSize(1));
  }

  @Test
  public void testNonSynchronous() throws InterruptedException {
    assertThat(latchNonSync.await(7000, MILLISECONDS), is(true));
    probe(PROBE_TIMEOUT, 100, () -> {
      assertThat(fooNonSync, hasSize(greaterThanOrEqualTo(2)));
      return true;
    });
  }

  public static Object addFooNonSync(String payload) {
    latchNonSync.countDown();

    synchronized (fooNonSync) {
      fooNonSync.add(payload);
      try {
        shutDownLatch.await(10000, MILLISECONDS);
      } catch (InterruptedException e) {

      }
    }

    return payload;
  }

  public static Object addFooSync(String payload) {
    latchSync.countDown();

    synchronized (fooSync) {
      fooSync.add(payload);
      try {
        shutDownLatch.await(10000, MILLISECONDS);
      } catch (InterruptedException e) {

      }
    }

    return payload;
  }

}
