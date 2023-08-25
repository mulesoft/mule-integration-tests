/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.schedule;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(SCHEDULER)})
@Story(DSL_VALIDATION_STORY)
@RunnerDelegateTo(Parameterized.class)
public class SchedulerConfigurationFailuresLazyInitTestCase extends MuleArtifactFunctionalTestCase {

  @Parameters(name = "configFile: {0}")
  public static Collection<String> params() {
    return asList("core-scheduler-no-scheduling-strategy.xml",
                  "pet-store-scheduler-no-scheduling-strategy.xml");
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  private final String configFile;

  public SchedulerConfigurationFailuresLazyInitTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/invalid/" + configFile;
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Test
  public void schedulerNoSchedulingStrategy() throws Exception {
    lazyComponentInitializer.initializeComponent(builder().globalName("scheduledFlow").build());
  }

}
