/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.fail;
import org.mule.runtime.api.exception.MuleException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class LogCheckTestCase extends AbstractIntegrationTestCase {

  private static final String VERBOSE_EXCEPTIONS_PROPERTY = "mule.verbose.exceptions";
  private static final String[] FLOWS_EXPECTING_FAILURE_NO_VERBOSE_EXCEPTIONS = {"checkStacktrace",};
  private static final String[] FLOWS_EXPECTING_SUCCESS_NO_VERBOSE_EXCEPTIONS = {"checkEquals", "checkSummary"};
  private static final String[] FLOWS_EXPECTING_FAILURE_WITH_VERBOSE_EXCEPTIONS = {};
  private static final String[] FLOWS_EXPECTING_SUCCESS_WITH_VERBOSE_EXCEPTIONS =
      {"checkEqualsVerbose", "checkStacktrace", "checkSummary", "allChecksTogetherNoneFailing"};

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-config.xml";
  }

  @Test
  public void runSuccessesVerboseExceptions() throws Exception {
    runSuccesses(true, FLOWS_EXPECTING_SUCCESS_WITH_VERBOSE_EXCEPTIONS);
  }

  @Test
  public void runSuccessesNoVerboseExceptions() throws Exception {
    runSuccesses(false, FLOWS_EXPECTING_SUCCESS_NO_VERBOSE_EXCEPTIONS);
  }

  @Test
  public void runFailuresVerboseExceptions() throws Exception {
    runFailures(true, FLOWS_EXPECTING_FAILURE_WITH_VERBOSE_EXCEPTIONS);
  }

  @Test
  public void runFailuresNoVerboseExceptions() throws Exception {
    runFailures(false, FLOWS_EXPECTING_FAILURE_NO_VERBOSE_EXCEPTIONS);
  }

  private void runSuccesses(boolean verboseExceptions, String[] flowNames) throws Exception {
    setVerboseExceptions(verboseExceptions);
    for (String flowName : flowNames) {
      flowRunner(flowName).run();
    }

  }

  private void runFailures(boolean verboseExceptions, String[] flowNames) throws Exception {
    setVerboseExceptions(verboseExceptions);
    for (String flowName : flowNames) {
      try {
        flowRunner(flowName).run();
      } catch (AssertionError e) {
        continue;
      }
      fail(String.format("flow \"%s\" was expected to fail but succeeded", flowName));
    }
  }

  private void setVerboseExceptions(boolean value) {
    System.setProperty(VERBOSE_EXCEPTIONS_PROPERTY, Boolean.toString(value));
    MuleException.refreshVerboseExceptions();
  }

}
