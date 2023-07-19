/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.test.AbstractIntegrationTestCase;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_MAPPINGS;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
@Story(ERROR_MAPPINGS)
public class ValidationModuleErrorHandlingTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/validation-module-error-handling.xml";
  }

  @Test
  @Issue("MULE-19139")
  public void validationAllWithErrorMapping() throws Exception {
    flowRunner("validationAllWithErrorMapping").runExpectingException(errorType("APP", "NULL"));
  }

  @Test
  @Issue("MULE-19139")
  public void validationWithErrorMapping() throws Exception {
    flowRunner("validationWithErrorMapping").runExpectingException(errorType("APP", "NULL"));
  }

}
