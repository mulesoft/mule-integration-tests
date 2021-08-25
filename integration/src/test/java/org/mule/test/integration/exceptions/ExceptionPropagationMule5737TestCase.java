/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_CONTINUE;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Assert that flows do not propagate exceptions via runFlow or use of flow-ref. Also assert that a sub-flow/processor-chain does
 * not handle it's own exception but they are rather handled by calling flow.
 */
@Issue("MULE-5737")
@Feature(ERROR_HANDLING)
@Story(ON_ERROR_CONTINUE)
public class ExceptionPropagationMule5737TestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-propagation-mule-5737-config.xml";
  }

  @Before
  public void before() {
    parentCaught = false;
    childCaught = true;
  }

  @Test
  public void testRequestResponseEndpointExceptionPropagation() throws Exception {
    expectedError.expectErrorType("TEST", "EXPECTED");
    runFlow("flow");
  }

  @Test
  public void testFlowWithChildFlowExceptionPropagation() throws Exception {
    runFlow("flowWithChildFlow");

    assertThat(parentCaught, is(false));
    assertThat(childCaught, is(true));
  }

  @Test
  public void testFlowWithSubFlowExceptionPropagation() throws Exception {
    runFlow("flowWithSubFlow");

    assertThat(parentCaught, is(true));
  }

  @Test
  public void testFlowWithChildServiceExceptionPropagation() throws Exception {
    runFlow("flowWithChildService");

    assertThat(parentCaught, is(false));
    assertThat(childCaught, is(true));
  }

  static boolean parentCaught;
  static boolean childCaught;

  public static Object senseParent(String payload) {
    parentCaught = true;

    return payload;
  }

  public static Object senseChild(String payload) {
    childCaught = true;

    return payload;
  }

}
