/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.exception.EventProcessingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FlowRunnerTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/functional/junit4/flow-runner-config.xml";
  }

  @Test
  public void flowFinishesSuccessfullyWhenExpectingException() throws Exception {
    Exception exception = flowRunner("okFailFlow").runExpectingException();
    assertNotNull(exception);
  }

  @Test
  public void flowRunFailsEvenWhenExpectingException() throws Exception {
    exception.expect(AssertionError.class);
    exception.expectMessage("evaluated false");
    final EventProcessingException exception = flowRunner("badFailFlow").runExpectingException();
    throw exception;
  }

}
