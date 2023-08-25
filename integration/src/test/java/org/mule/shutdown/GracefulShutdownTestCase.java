/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.shutdown;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
@RunnerDelegateTo(Parameterized.class)
public class GracefulShutdownTestCase extends AbstractIntegrationTestCase {

  @Parameters(name = "{0}")
  public static List<String> parameters() {
    return asList("org/mule/shutdown/flow-with-operation.xml",
                  "org/mule/shutdown/flow-with-failing-operation.xml",
                  "org/mule/shutdown/flow-with-flow-ref.xml",
                  "org/mule/shutdown/flow-with-operation-in-error-handler.xml",
                  "org/mule/shutdown/flow-with-tx-flow-ref.xml",
                  "org/mule/shutdown/flow-with-tx-scope.xml",
                  "org/mule/shutdown/flow-with-scatter-gather-and-flow-ref.xml");
  }

  @Rule
  public SystemProperty propagateDisposeError = new SystemProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR, "");

  private final String configFile;

  public GracefulShutdownTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  @Description("Verify that the graceful shutdown occurs in a timely manner")
  public void flowStopTimelyManner() {
    for (int i = 0; i < getRuntime().availableProcessors() * 4; ++i) {
      try {
        flowRunner("flow").run();
      } catch (Exception e) {
        // Nothing to do
      }
    }
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

}
