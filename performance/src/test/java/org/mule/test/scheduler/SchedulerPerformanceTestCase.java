/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.scheduler;

import static org.mule.service.scheduler.internal.config.ContainerThreadPoolsConfig.BIG_POOL_DEFAULT_SIZE;
import static org.mule.tck.junit4.matcher.Eventually.eventually;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.service.scheduler.internal.DefaultSchedulerService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class SchedulerPerformanceTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void negativeScheduleTime() throws Exception {
    DefaultSchedulerService schedulerService = new DefaultSchedulerService();
    schedulerService.start();

    Scheduler scheduler = schedulerService.ioScheduler();
    scheduler.schedule(() -> System.out.println("Executing MAX X"), Long.MAX_VALUE, NANOSECONDS);
    scheduler.schedule(() -> System.out.println("Executing 10ms"), 10, MILLISECONDS);
    scheduler.schedule(() -> System.out.println("Executing 10ms"), 10, MILLISECONDS);
    scheduler.schedule(() -> System.out.println("Executing 10ms"), 10, MILLISECONDS);
    scheduler.schedule(() -> System.out.println("Executing Immediate"), 0, NANOSECONDS);

    Thread.sleep(10000);

    scheduler.shutdown();
    schedulerService.stop();
  }

  @Test
  public void withManyAndRee() throws Exception {
    test(300, 300_000L, true);
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
    ExecutorService schedulerThatSchedules = Executors.newFixedThreadPool(testSchedulerSize);
    AtomicLong counter = new AtomicLong();

    DefaultSchedulerService schedulerService = new DefaultSchedulerService();
    schedulerService.start();

    Scheduler scheduler = schedulerService.ioScheduler();
    // Schedule some slow tasks to force temporary RejectedExecutionException's in the doSchedule() method
    if (forceRejections) {
      scheduleSlowTasks(scheduler, BIG_POOL_DEFAULT_SIZE);
    }

    // Many tasks with delay=0, and Many/100 tasks to be executed in 10 years...
    for (long i = 0; i < parallelSchedulesNumber; i++) {
      if (i % 100 == 0) {
        schedulerThatSchedules
            .submit(() -> scheduler.schedule(counter::incrementAndGet, 365_000, DAYS));
      }
      schedulerThatSchedules.submit(() -> scheduler.schedule(counter::incrementAndGet, 0, MILLISECONDS));
    }

    // The "immediate" tasks are eventually executed, we won't wait for 10 years...
    assertThat(counter, is(eventually(equalTo(parallelSchedulesNumber)).atMostIn(50, SECONDS)));

    scheduler.shutdown();
    schedulerService.stop();
    schedulerThatSchedules.shutdown();
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

  private TypeSafeMatcher<AtomicLong> equalTo(long number) {
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
