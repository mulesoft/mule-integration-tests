/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import io.qameta.allure.Feature;

/**
 * <p>
 * Uses the API to stop and start cron schedulers.
 * </p>
 */
@Feature(SCHEDULER)
public class CronSchedulerTestCase extends AbstractSchedulerTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  @ClassRule
  public static SystemProperty days = new SystemProperty("expression.property", "0/2 * * * * ?");

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-config.xml";
  }

  @Before
  public void before() {
    foo.clear();
    bar.clear();
  }

  @Test
  public void test() throws Exception {
    new PollingProber(3000, 100).check(new JUnitLambdaProbe(() -> {
      checkForFooCollectionToBeFilled();
      checkForBarCollectionToBeFilled();
      return true;
    }));

    stopSchedulers();

    int fooElementsAfterStopping = foo.size();

    sleep(3000);
    assertThat(foo, hasSize(fooElementsAfterStopping));

    startSchedulers();
    runSchedulersOnce();

    new PollingProber(3000, 100).check(new JUnitLambdaProbe(() -> {
      // One for the scheduler run and another for the on-demand one
      assertThat(foo, hasSize(fooElementsAfterStopping + 2));
      return true;
    }));
  }

  private void checkForFooCollectionToBeFilled() {
    synchronized (foo) {
      foo.size();
      assertThat(foo, hasSize(greaterThan(0)));
      for (String s : foo) {
        assertEquals("foo", s);
      }
    }
  }

  private void checkForBarCollectionToBeFilled() {
    synchronized (bar) {
      bar.size();
      assertThat(bar, hasSize(greaterThan(0)));
      for (String s : bar) {
        assertEquals("bar", s);
      }
    }
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

  public static class Foo extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      synchronized (foo) {
        if (foo.size() < 10) {
          foo.add((String) event.getMessage().getPayload().getValue());
        }
      }
    }
  }

  public static class Bar extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      synchronized (bar) {

        if (bar.size() < 10) {
          bar.add((String) event.getMessage().getPayload().getValue());
        }
      }
    }
  }
}
