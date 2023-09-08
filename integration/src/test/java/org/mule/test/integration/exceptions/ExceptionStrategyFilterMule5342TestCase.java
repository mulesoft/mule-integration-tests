/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.functional.api.exception.ExpectedError.none;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class ExceptionStrategyFilterMule5342TestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedException = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-filter-mule-5342.xml";
  }

  @Test
  public void exceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception {
    expectedException.expectErrorType("MULE", "ANY");
    flowRunner("filter").withPayload(TEST_MESSAGE).run();
  }

}
