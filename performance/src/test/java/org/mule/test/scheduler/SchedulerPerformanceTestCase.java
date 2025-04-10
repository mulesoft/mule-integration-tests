/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.scheduler;

import static org.mule.service.scheduler.internal.config.ContainerThreadPoolsConfig.BIG_POOL_DEFAULT_SIZE;
import static org.mule.tck.junit4.matcher.Eventually.eventually;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.service.scheduler.internal.DefaultSchedulerService;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class SchedulerPerformanceTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void withManyAndRee() throws Exception {
    test(500, 500_000L, true);
  }

  @Test
  public void withFewAndRee() throws Exception {
    test(30, 30L, true);
  }

  @Test
  public void withManyNoRee() throws Exception {
    test(300, 300_000L, false);
  }

  @Test
  public void withFewNoRee() throws Exception {
    test(30, 30L, false);
  }

  private void test(int testSchedulerSize, long parallelSchedulesNumber, boolean forceRejections) throws Exception {
    ExecutorService schedulerThatSchedules = newFixedThreadPool(testSchedulerSize);
    AtomicLong counter = new AtomicLong();

    DefaultSchedulerService schedulerService = new DefaultSchedulerService();
    schedulerService.start();

    Scheduler scheduler = schedulerService.ioScheduler();

    try {
      // Schedule some slow tasks to force temporary RejectedExecutionException's in the doSchedule() method
      if (forceRejections) {
        scheduleSlowTasks(scheduler, BIG_POOL_DEFAULT_SIZE);
      }

      // Many tasks with delay=0, and Many tasks to be executed infinitely in the future...
      for (long i = 0; i < parallelSchedulesNumber; i++) {
        schedulerThatSchedules.submit(() -> scheduler.schedule(counter::incrementAndGet, MAX_VALUE, NANOSECONDS));
        schedulerThatSchedules.submit(() -> scheduler.schedule(counter::incrementAndGet, 0, MILLISECONDS));
      }

      // The "immediate" tasks are eventually executed, we won't wait infinitely for the others...
      assertThat(counter, is(eventually(equalToLong(parallelSchedulesNumber)).atMostIn(50, SECONDS)));
    } catch (AssertionError e) {
      System.out.println("ISSUE REPRODUCED!!! Expected " + parallelSchedulesNumber + " but got " + counter.get());
      dumpThreads();
      throw e;
    } finally {
      scheduler.shutdown();
      schedulerService.stop();
      schedulerThatSchedules.shutdown();

    }

  }

  private static void dumpThreads() {
    final StringBuilder buf = new StringBuilder(8192);
    for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
      buf.append(info);
    }
    buf.append('\n');
    System.err.println(buf.toString());
  }

  private static void scheduleSlowTasks(Scheduler scheduler, long howMany) {
    for (int i = 0; i < howMany; i++) {
      scheduler.execute(() -> {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private TypeSafeMatcher<AtomicLong> equalToLong(long number) {
    return new TypeSafeMatcher<AtomicLong>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected to be this number ").appendValue(number);
      }

      @Override
      protected boolean matchesSafely(AtomicLong item) {
        return item.get() == number;
      }
    };
  }
}
