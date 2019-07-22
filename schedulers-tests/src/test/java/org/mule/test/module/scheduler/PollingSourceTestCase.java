/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.petstore.extension.PetAdoptionSource.ALL_PETS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractSchedulerTestCase;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class PollingSourceTestCase extends AbstractSchedulerTestCase {

  @Override
  protected String getConfigFile() {
    return "polling-source-config.xml";
  }

  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();

  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        System.out.println("Adopting");
        ADOPTION_EVENTS.add(event);
      }
      return event;
    }
  }

  /* This test checks that when a polling source with a fixed frequency scheduler with start delay is restarted, the
  start delay is not applied again. The polling source of this test is set to fail midway through populating the pet
  adoption list, which will provoke a restart. Without the changes made in MULE-16974, this test would fail, because the
  start delay would be re-applied on the restart and the probe would timeout. */
  @Test
  public void whenReconnectingAfterConnectionExceptionSchedulerRunsWithoutStartDelay() throws Exception {
    startFlow("fixedFrequencyReconnectingPoll");
    assertAllPetsAdopted();
  }

  /* This test checks that when a polling source with a cron scheduler is restarted inside the time window where the
  cron expression evaluates to true (in this case within the duration of a second), the wrapper does not add an extra
  run of the task because the cron scheduler will naturally evaluate to true and run the task.
  Schedulers with a cron expression such as "0/3 * * * * ?" don't exactly run every 3s, but rather if the current second
  equals 0 or 3 or 6 or 9, etc., so if a scheduler runs a polling task at the start of second 6, which fails and triggers
  a reconnection, creating a new scheduler before the end of second 6, this new scheduler will run because it does not have
  memory or context of what happened before, i.e. that the task was already triggered for second 6. This is what makes
  these tests so tricky.
  In this test, source is set to poll every 3000ms, where it adopts a pet and sets a timestamp for the adoption. On the third
  poll, suppose it happens at the beginning of the second 6, the source will fail before adopting a pet, triggering a
  reconnection. The newly created cron scheduler will check if it needs to run and it will run because the reconnection
  happened under a second (it still is second 6), but the scheduler wrapper will not add an extra polling task because
  of this. Therefore, the difference between all adoptions will be less than 3200ms.
  This scenario can more realistically happen when the cron expression is set to run at a certain minute. If there
  is a reconnection within that minute, then the scheduler will run again without the need of the changes made in
  MULE-16974. However, in cases where the reconnection does not happen within the time window of the cron expression,
  we do want to add an extra run of the task (this is checked by another test). */
  @Test
  public void whenReconnectingInsideTheTimeWindowOfCronExpressionTheWrapperDoesNotAddARun() throws Exception {
    startFlow("cronReconnectingPoll");
    assertAdoptionTimestampsInside();
  }

  /* This test checks that when a polling source with a cron scheduler is restarted outside the time window where the
  cron expression evaluates to true, the wrapper adds an immediate run of the polling task and then the polling keeps
  being run in the interval set by the cron expression. The source is set to poll every 3000ms, where
  it adopts a pet and sets a timestamp for the adoption. On the third poll, the source will fail before adopting
  a pet, sleeping for 1.1s and then triggering a reconnection. The sleep time is added so that the reconnection happens
  outside the time window where the cron expression evaluates to true. Before the changes made in MULE-16974, this would
  mean that the difference between the second and third polls would be of 6000ms. With the changes, an extra run is
  triggered, so the difference between the 2nd and 3rd polls will be less than 4300ms (3000ms between polls + 1100ms of
  sleep time + 200ms of buffer), which is less than the 6000ms it would take before the changes, so the test proves that an
  extra task is added to run once immediately when the restart happens outside the cron expression time window. */
  @Test
  public void whenReconnectingOutsideTheTimeWindowOfCronExpressionTheWrapperAddsAnImmediateRun() throws Exception {
    startFlow("cronReconnectingPollWithSleep");
    assertAdoptionTimestampsOutside();
  }

  private void assertAdoptionTimestampsOutside() throws Exception {
    Thread.sleep(25000);
    List<Instant> timestampList =
        ADOPTION_EVENTS.stream().map(e -> (Instant) e.getMessage().getAttributes().getValue()).collect(toList());
    Collections.sort(timestampList);
    for (int i = 0; i < timestampList.size() - 1; i++) {
      Long difference = timestampList.get(i).until(timestampList.get(i + 1), MILLIS);
      System.out.println(difference);
      if (i == 1) {
        assertThat(difference, is(lessThan(4300L)));
      } else if (i == 2) {
        assertThat(difference, is(lessThan(2100L)));
      } else {
        assertThat(difference, is(lessThan(3200L)));
      }
    }
  }

  private void assertAdoptionTimestampsInside() throws Exception {
    Thread.sleep(25000);
    List<Instant> timestampList =
        ADOPTION_EVENTS.stream().map(e -> (Instant) e.getMessage().getAttributes().getValue()).collect(toList());
    Collections.sort(timestampList);
    for (int i = 0; i < timestampList.size() - 1; i++) {
      Long difference = timestampList.get(i).until(timestampList.get(i + 1), MILLIS);
      System.out.println(difference);
      assertThat(difference, is(lessThan(3200L)));
    }
  }

  private void assertAllPetsAdopted() {
    check(5000, 200, () -> {
      synchronized (ADOPTION_EVENTS) {
        return ADOPTION_EVENTS.size() >= ALL_PETS.size() &&
            ADOPTION_EVENTS.stream().map(e -> e.getMessage().getPayload().getValue().toString()).collect(toList())
                .containsAll(ALL_PETS);
      }
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
