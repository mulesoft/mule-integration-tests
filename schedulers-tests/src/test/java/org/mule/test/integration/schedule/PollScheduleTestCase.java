/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import static java.lang.Thread.sleep;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SchedulerStories.SCHEDULED_FLOW_EXECUTION;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * This is a test for poll with schedulers. It validates that the polls can be executed, stopped, run.
 */
@Feature(SCHEDULER)
@Story(SCHEDULED_FLOW_EXECUTION)
public class PollScheduleTestCase extends AbstractSchedulerTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  @ClassRule
  public static SystemProperty days = new SystemProperty("frequency.days", "4");

  @ClassRule
  public static SystemProperty millis = new SystemProperty("frequency.millis", "2000");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/polling-schedule-config.xml";
  }

  /**
   * This test validates that the polls can be stopped and run on demand.
   *
   * It checks correct functionality of polls. Stop the schedulers Waits for the polls to be executed (they shouldn't, as they are
   * stopped) Checks that the polls where not executed. Runs the polls on demand Checks that the polls where executed only once.
   */
  @Test
  public void test() throws Exception {
    new PollingProber(10000, 100l).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return (foo.size() > 2 && checkCollectionValues(foo, "foo")) && (bar.size() > 2 && checkCollectionValues(bar, "bar"));
      }

      @Override
      public String describeFailure() {
        return "The collections foo and bar are not correctly filled";
      }
    });

    stopSchedulers();

    int fooElementsAfterStopping = foo.size();

    sleep(2000);

    assertThat(foo, hasSize(fooElementsAfterStopping));

    startSchedulers();
    runSchedulersOnce();

    new PollingProber(200, 10).check(new JUnitLambdaProbe(() -> {
      // One for the scheduler run and another for the on-demand one
      assertThat(foo, hasSize(fooElementsAfterStopping + 2));
      return true;
    }));
  }

  private boolean checkCollectionValues(List<String> coll, String value) {
    for (String s : coll) {
      if (!s.equals(value)) {
        return false;
      }
    }

    return true;
  }


  private void runSchedulersOnce() throws Exception {
    locator.find(builderFromStringRepresentation("pollfoo/source").build()).map(source -> (SchedulerMessageSource) source)
        .ifPresent(SchedulerMessageSource::trigger);
  }

  private void stopSchedulers() throws MuleException {
    registry.<Stoppable>lookupByName("pollfoo").get().stop();
  }

  private void startSchedulers() throws MuleException {
    registry.<Startable>lookupByName("pollfoo").get().start();
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
