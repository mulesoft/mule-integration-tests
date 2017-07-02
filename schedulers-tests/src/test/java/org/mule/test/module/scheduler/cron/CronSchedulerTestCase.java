/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;


/**
 * <p>
 * Uses the API to stop and start cron schedulers.
 * </p>
 */
public class CronSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  private static List<String> foo = new ArrayList<>();
  private static List<String> bar = new ArrayList<>();

  @ClassRule
  public static SystemProperty days = new SystemProperty("expression.property", "0/1 * * * * ?");

  @Override
  protected String getConfigFile() {
    return "cron-scheduler-config.xml";
  }

  @Test
  public void test() throws Exception {
    waitForPollElements();

    checkForFooCollectionToBeFilled();
    checkForBarCollectionToBeFilled();

    stopSchedulers();

    int fooElementsAfterStopping = foo.size();

    waitForPollElements();

    assertThat(foo.size(), is(fooElementsAfterStopping));

    startSchedulers();
    runSchedulersOnce();

    new PollingProber(2000, 100).check(new JUnitLambdaProbe(() -> {
      // One for the scheduler run and another for the on-demand one
      assertThat(foo.size(), is(fooElementsAfterStopping + 2));
      return true;
    }));
  }

  private void waitForPollElements() throws InterruptedException {
    Thread.sleep(2000);
  }

  private void checkForFooCollectionToBeFilled() {
    synchronized (foo) {
      foo.size();
      assertTrue(foo.size() > 0);
      for (String s : foo) {
        assertEquals("foo", s);
      }
    }
  }

  private void checkForBarCollectionToBeFilled() {
    synchronized (bar) {
      bar.size();
      assertTrue(bar.size() > 0);
      for (String s : bar) {
        assertEquals("bar", s);
      }
    }
  }

  private void runSchedulersOnce() throws Exception {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    MessageSource flowSource = flow.getSource();
    if (flowSource instanceof SchedulerMessageSource) {
      ((SchedulerMessageSource) flowSource).trigger();
    }
  }

  private void stopSchedulers() throws MuleException {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.stop();
  }

  private void startSchedulers() throws MuleException {
    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollfoo"));
    flow.start();
  }

  public static class Foo implements EventCallback {

    @Override
    public void eventReceived(Event event, Object component, MuleContext muleContext) throws Exception {
      synchronized (foo) {

        if (foo.size() < 10) {
          foo.add((String) event.getMessage().getPayload().getValue());
        }
      }
    }
  }

  public static class Bar implements EventCallback {

    @Override
    public void eventReceived(Event event, Object component, MuleContext muleContext) throws Exception {
      synchronized (bar) {

        if (bar.size() < 10) {
          bar.add((String) event.getMessage().getPayload().getValue());
        }
      }
    }
  }
}
