/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;
import static org.mule.test.petstore.extension.PetFailingPollingSource.STARTED_POLLS;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(SCHEDULER)
public class CronPollingSourceTestCase extends AbstractSchedulerTestCase {

  private static final int PROBE_TIMEOUT = 10000;
  private static final int PROBE_FREQUENCY = 200;

  @Override
  protected String getConfigFile() {
    return "polling-source-config.xml";
  }

  @Before
  public void resetCounter() {
    STARTED_POLLS = 0;
  }

  @Test
  public void cronExpressionPollIsRetriggeredOnReconnection() throws Exception {
    // Starts a flow with a polling source which uses a cron expression that triggers avery 5 seconds
    startFlow("poolWithCronReconnection");

    long start = currentTimeMillis();

    // We wait until three polls are started
    // The 3 polls will be initiated before 10 seconds passes, this means that one of the polls encountered a connection issue and
    // retriggered.
    assertFailingSourceStartedPolls(3);
  }

  private void assertFailingSourceStartedPolls(int polls) {
    check(PROBE_TIMEOUT, PROBE_FREQUENCY, () -> {
      assertThat(STARTED_POLLS, is(polls));
      return true;
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

}
