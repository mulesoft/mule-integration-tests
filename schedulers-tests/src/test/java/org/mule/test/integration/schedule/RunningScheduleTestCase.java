/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.schedule;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SchedulerStories.SCHEDULED_FLOW_EXECUTION;

import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * This test checks that a Scheduler can be stopped, executed and started. Also shows how a customer can set his own scheduler in
 * mule config.
 *
 * It also shows the way users can add a new Scheduler as a spring bean.
 */
@Feature(SCHEDULER)
@Story(SCHEDULED_FLOW_EXECUTION)
public class RunningScheduleTestCase extends AbstractSchedulerTestCase {

  public static final String SCHEDULER_NAME = "testScheduler";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/schedule/scheduler-config.xml";
  }

  @Test
  public void test() throws Exception {
    MockScheduler scheduler = findScheduler(SCHEDULER_NAME);
    new PollingProber(2000, 50).check(new JUnitLambdaProbe(() -> {
      assertTrue(scheduler.getCount() > 0);
      return true;
    }));

    stopSchedulers();

    Thread.sleep(2000);

    int count = scheduler.getCount();

    new PollingProber(2000, 100).check(new JUnitLambdaProbe(() -> {
      assertThat(scheduler.getCount(), is(count));
      return true;
    }));
  }

  private void stopSchedulers() throws MuleException {
    findScheduler(SCHEDULER_NAME).stop();
  }

  private MockScheduler findScheduler(String schedulerName) {
    return (MockScheduler) registry.lookupByName(schedulerName).get();
  }
}
