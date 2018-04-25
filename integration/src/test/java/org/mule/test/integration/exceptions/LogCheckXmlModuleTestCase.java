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
import org.junit.rules.ExpectedException;

public class LogCheckXmlModuleTestCase extends AbstractIntegrationTestCase {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static SystemProperty verboseExceptions = new SystemProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(false));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @AfterClass
  public static void afterClass() {
    refreshVerboseExceptions();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-xml-module-config.xml";
  }

  @Rule
  public SystemProperty logFlowStack = new SystemProperty(MULE_FLOW_TRACE, Boolean.toString(false));

  @Test
  public void runXmlSdkOperationError() throws Exception {
    runSuccesses(true, "xmlSdkOperationError");
  }

  @Test
  public void runXmlSdkOperationErrorNested() throws Exception {
    runSuccesses(true, "xmlSdkOperationErrorNested");
  }

  private void runSuccesses(boolean verboseExceptions, String flowName) throws Exception {
    System.setProperty(MULE_VERBOSE_EXCEPTIONS, Boolean.toString(verboseExceptions));
    refreshVerboseExceptions();
    flowRunner(flowName).run();
  }

}
