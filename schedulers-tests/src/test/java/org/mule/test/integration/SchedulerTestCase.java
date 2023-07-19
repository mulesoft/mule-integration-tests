/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SchedulerStories.SCHEDULED_FLOW_EXECUTION;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(SCHEDULER)
@Story(SCHEDULED_FLOW_EXECUTION)
public class SchedulerTestCase extends AbstractSchedulerTestCase {

  private static List<String> foo;
  private static List<String> bar;
  private static List<CoreEvent> events;
  private static List<String> eventIds;

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    foo = new ArrayList<>();
    bar = new ArrayList<>();
    events = new ArrayList<>();
    eventIds = new ArrayList<>();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-config.xml";
  }

  @Test
  public void testPolling() throws Exception {
    assertThat(locator.find(buildFromStringRepresentation("scheduler")).stream()
        .filter(source -> source instanceof SchedulerMessageSource).count(), is(4L));

    new PollingProber(100, RECEIVE_TIMEOUT).check(new JUnitLambdaProbe(() -> {
      assertThat(foo, hasSize(greaterThan(0)));
      assertThat(bar, hasSize(greaterThan(0)));
      assertThat(events, hasSize(greaterThan(0)));
      return true;
    }));

    synchronized (foo) {
      assertTrue(foo.size() > 0);
      for (String s : foo) {
        assertEquals("foo", s);
      }
    }
    synchronized (bar) {
      assertTrue(bar.size() > 0);
      for (String s : bar) {
        assertEquals("bar", s);
      }
    }

    synchronized (events) {
      assertTrue(events.size() > 0);
      assertEquals(events.size(), eventIds.size());

      for (int i = 0; i < events.size(); i++) {
        assertThat(i + ", " + events.toString(), events.get(i), not(nullValue()));
        assertThat(eventIds.get(i), equalTo(events.get(i).getContext().getId()));
      }
    }
  }

  public static class EventWireTrap extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (events) {
        events.add(event);
        eventIds.add(event.getContext().getId());
      }
      return event;
    }
  }

  public static Object addFoo(String payload) {
    synchronized (foo) {
      if (foo.size() < 10) {
        foo.add(payload);
      }
    }
    return payload;
  }

  public static Object addBar(String payload) {
    synchronized (bar) {
      if (bar.size() < 10) {
        bar.add(payload);
      }
    }
    return payload;
  }
}
