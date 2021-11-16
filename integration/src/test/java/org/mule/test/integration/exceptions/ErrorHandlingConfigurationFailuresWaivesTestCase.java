/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(DSL_VALIDATION_STORY)
public class ErrorHandlingConfigurationFailuresWaivesTestCase extends AbstractConfigurationFailuresTestCase {

  @Test
  @Issue("MULE-19879")
  public void nonExistantErrorValidation() throws Exception {
    // verify this doesn't fail on initialize
    loadConfiguration("org/mule/test/integration/exceptions/global-unreferenced-invalid-error-handler.xml");
  }

}
