/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractSchedulerTestCase;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SharedSchedulersTestCase extends AbstractSchedulerTestCase {

  @Override
  protected String getConfigFile() {
    return "polling-source-config.xml";
  }

  private static final List<CoreEvent> ADOPTION_EVENTS = new LinkedList<>();
  private static final List<Instant> ADOPTION_TIMES = new LinkedList<>();


  public static class AdoptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (ADOPTION_EVENTS) {
        ADOPTION_EVENTS.add(event);
        ADOPTION_TIMES.add(Instant.now());
      }
      return event;
    }
  }

  @Before
  public void before() {
    ADOPTION_EVENTS.clear();
    ADOPTION_TIMES.clear();
  }

  @Test
  public void pollingSourceUses500msScheduler() throws Exception {
    startFlow("pollEvery500ms");
    assertAdoptionTimestamps500msScheduler();
  }

  @Test
  public void pollingSourceUses1000msScheduler() throws Exception {
    startFlow("pollEvery1000ms");
    assertAdoptionTimestamps1000msScheduler();
  }

  private void assertAdoptionTimestamps500msScheduler() throws Exception {
    Thread.sleep(15000);
    for (int i = 0; i < ADOPTION_TIMES.size() - 1; i++) {
      Long difference = ADOPTION_TIMES.get(i).until(ADOPTION_TIMES.get(i + 1), MILLIS);
      System.out.println(difference);
      assertThat(difference, is(lessThan(600L)));
    }
  }

  private void assertAdoptionTimestamps1000msScheduler() throws Exception {
    Thread.sleep(15000);
    for (int i = 0; i < ADOPTION_TIMES.size() - 1; i++) {
      Long difference = ADOPTION_TIMES.get(i).until(ADOPTION_TIMES.get(i + 1), MILLIS);
      System.out.println(difference);
      assertThat(difference, is(both(greaterThan(800L)).and(lessThan(1200L))));
    }
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
