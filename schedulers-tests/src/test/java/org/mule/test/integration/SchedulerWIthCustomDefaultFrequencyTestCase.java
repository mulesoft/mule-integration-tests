/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_SCHEDULER_FIXED_FREQUENCY;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractSchedulerTestCase;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(SCHEDULER)
public class SchedulerWIthCustomDefaultFrequencyTestCase extends AbstractSchedulerTestCase {

  private static final long CUSTOM_FREQ = 42;

  @Rule
  public SystemProperty defaultConfigProperty =
      new SystemProperty(DEFAULT_SCHEDULER_FIXED_FREQUENCY, String.valueOf(CUSTOM_FREQ));

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-with-custom-default-config.xml";
  }

  @Test
  @Issue("MULE-18262")
  public void customDefaultFreq() {
    SchedulerMessageSource source = (SchedulerMessageSource) locator.find(buildFromStringRepresentation("scheduler")).get(0);
    FixedFrequencyScheduler conf = (FixedFrequencyScheduler) source.getConfiguration();
    assertThat(conf.getFrequency(), is(CUSTOM_FREQ));
  }
}
