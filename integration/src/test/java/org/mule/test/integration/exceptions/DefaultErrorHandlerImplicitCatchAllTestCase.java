/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.DEFAULT_ERROR_HANDLER;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(DEFAULT_ERROR_HANDLER)
public class DefaultErrorHandlerImplicitCatchAllTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-catch-all.xml";
  }

  @Test
  public void defaultHandlerIsUsed() throws Exception {
    assertThat(flowRunner("connectivity").withPayload("").run().getMessage(), hasPayload(equalTo("append")));
  }

  @Test
  public void propagateAllIsInjected() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    flowRunner("expression").withPayload("").run();
  }

}
