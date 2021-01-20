/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Ignore;
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
  @Ignore
  public void validationAllWithErrorMapping() throws Exception {
    flowRunner("validationAllWithErrorMapping").runExpectingException(errorType("TEST", "NULL"));
  }

  @Test
  @Issue("MULE-19139")
  @Ignore
  public void validationWithErrorMapping() throws Exception {
    flowRunner("validationWithErrorMapping").runExpectingException(errorType("TEST", "NULL"));
  }

}
