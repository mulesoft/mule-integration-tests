/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.ERROR_REPORTING;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;

import static org.junit.runners.Parameterized.Parameters;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@Feature(LOGGING)
@Story(ERROR_REPORTING)
public class ForkJoinRoutersLogCheckTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty legacyCompositeRoutingExceptionLog;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/fork-join-routers-log-config.xml";
  }

  @Parameters(name = "Legacy log: {0}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {true},
                  new Object[] {false});
  }

  public ForkJoinRoutersLogCheckTestCase(boolean legacyCompositeRoutingExceptionLog) {
    this.legacyCompositeRoutingExceptionLog =
        new SystemProperty(MULE_PRINT_LEGACY_COMPOSITE_EXCEPTION_LOG, Boolean.toString(legacyCompositeRoutingExceptionLog));
  }

  @Test
  @Issue("W-10965130")
  public void compositeRoutingExceptionForParallelForEach() throws Exception {
    if (parseBoolean(legacyCompositeRoutingExceptionLog.getValue())) {
      runSuccesses("previousParallelForEachFlow");
    } else {
      runSuccesses("parallelForEachFlow");
    }
  }

  @Test
  @Issue("W-10965130")
  public void compositeRoutingExceptionForScatterGather() throws Exception {
    if (parseBoolean(legacyCompositeRoutingExceptionLog.getValue())) {
      runSuccesses("previousScatterGatherFlow");
    } else {
      runSuccesses("scatterGatherFlow");
    }
  }


  private void runSuccesses(String flowName) throws Exception {
    flowRunner(flowName).run();
  }

}
