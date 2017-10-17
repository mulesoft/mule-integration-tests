/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.runtime.api.exception.MuleException.MULE_VERBOSE_EXCEPTIONS;
import static org.mule.runtime.api.exception.MuleException.refreshVerboseExceptions;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_FLOW_TRACE;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class LogCheckTestCase extends AbstractIntegrationTestCase {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static SystemProperty verboseExceptions = new SystemProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(false));

  @AfterClass
  public static void afterClass() {
    refreshVerboseExceptions();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-config.xml";
  }

  @Rule
  public SystemProperty logFlowStack = new SystemProperty(MULE_FLOW_TRACE, Boolean.toString(false));

  @Test
  public void runVerboseCheckEqualsVerbose() throws Exception {
    runSuccesses(true, "checkEqualsVerbose");
  }

  @Test
  public void runVerboseCheckStacktrace() throws Exception {
    runSuccesses(true, "checkStacktrace");
  }

  @Test
  public void runVerboseCheckSummary() throws Exception {
    runSuccesses(true, "checkSummary");
  }

  @Test
  public void runVerboseAllChecksTogetherNoneFailing() throws Exception {
    runSuccesses(true, "allChecksTogetherNoneFailing");
  }

  @Test
  public void checkNonVerboseEquals() throws Exception {
    runSuccesses(false, "checkSummary");
  }

  @Test
  public void checkNonVerboseSummary() throws Exception {
    runSuccesses(false, "checkSummary");
  }

  @Test
  public void unknownSummaryShowsFilteredStack() throws Exception {
    runSuccesses(false, "unknownFiltered");
  }

  @Test
  public void unknownVerboseShowsFullStack() throws Exception {
    runSuccesses(true, "unknownFull");
  }

  private void runSuccesses(boolean verboseExceptions, String flowName) throws Exception {
    System.setProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(verboseExceptions));
    refreshVerboseExceptions();
    flowRunner(flowName).run();
  }

}
