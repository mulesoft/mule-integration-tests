/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.fail;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.SOURCE_MANAGEMENT;
import org.mule.runtime.api.event.Event;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SCHEDULER_SERVICE)
@Story(SOURCE_MANAGEMENT)
public class SchedulerManagementTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-management-config.xml";
  }

  @Description("scheduler that never runs due to configuration but works by triggering it manually")
  @Test
  public void triggerSchedulerManually() {
    SchedulerMessageSource scheduler = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("neverRunningScheduler").addSourcePart().build()).get();
    scheduler.trigger();

    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        Event response = queueManager.read("neverRunningSchedulerQueue", 100, MILLISECONDS);
        return response != null;
      }

      @Override
      public String describeFailure() {
        return "Message expected by triggering flow";
      }
    });
  }

  @Description("scheduler that runs once, gets stopped by a functional component within the same flow and the it's triggered manually")
  @Test
  public void stopSchedulerWithinFlowAndTriggerItManually() throws Exception {
    SchedulerMessageSource scheduler = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    AtomicInteger atomicInteger = new AtomicInteger(0);

    Latch componentExecutedLatch = new Latch();
    getFromFlow(locator, "schedulerControlledFromSameFlow").setEventCallback((eventContext, component, muleContext) -> {
      scheduler.stop();
      atomicInteger.incrementAndGet();
      componentExecutedLatch.release();
    });
    if (!componentExecutedLatch.await(RECEIVE_TIMEOUT, MILLISECONDS)) {
      fail("test component never executed");
    }

    scheduler.trigger();
    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return atomicInteger.get() == 2;
      }

      @Override
      public String describeFailure() {
        return "Executed two total executions of the flow but received " + atomicInteger.get();
      }
    });
  }

  @Description("scheduler start twice does not fail")
  @Test
  public void startTwiceDoesNotFail() throws MuleException {
    SchedulerMessageSource scheduler = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    scheduler.start();
    scheduler.start();
  }

  @Description("scheduler stop twice does not fail")
  @Test
  public void stopTwiceDoesNotFail() throws MuleException {
    SchedulerMessageSource scheduler = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(builder().globalName("schedulerControlledFromSameFlow").addSourcePart().build()).get();
    scheduler.stop();
    scheduler.stop();
  }

}
