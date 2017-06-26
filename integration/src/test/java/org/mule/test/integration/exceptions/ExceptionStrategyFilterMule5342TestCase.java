/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorType;
import static org.mule.tck.junit4.matcher.MessagingExceptionMatcher.withEventThat;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExceptionStrategyFilterMule5342TestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-filter-mule-5342.xml";
  }

  @Test
  public void exceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception {
    expectedException.expect(withEventThat(hasErrorType("MULE", "ANY")));
    flowRunner("filter").withPayload(TEST_MESSAGE).run();
  }

}
