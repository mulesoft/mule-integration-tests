/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

@Feature(ERROR_HANDLING)
@Story("Global Error handlers and 'circular references'")
public class GlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/global-error-handler.xml";
  }

  @Test
  @Description("Flow with error handler reference and name matching")
  public void errorHandlerWithSelfReference() throws Exception {
    // This should fail, but in 4.2 it didn't so we must accept it in 4.3, since it does work
    CoreEvent event = flowRunner("flowWithErrorHandlerSelfReferencing").run();
    assertThat(event.getMessage().getPayload().getValue(), is("Chocotorta"));
  }

  @Test
  @Description("Flow with error handler with on-error-continue reference and name matching")
  public void errorHandlerWithSelfReferenceToContinue() throws Exception {
    // This should fail, but in 4.2 it didn't so we must accept it in 4.3, since it does work
    CoreEvent event = flowRunner("flowWithErrorHandlerSelfReferencingContinue").run();
    assertThat(event.getMessage().getPayload().getValue(), is("Sachertorte"));
  }

  @Test
  @Description("Flow with error handler with on-error-continue reference and name matching, and matching on-error-continue in global error handler")
  public void errorHandlerWithSelfReferenceToContinueReferencedInGlobalEH() throws Exception {
    // This should fail, but in 4.2 it didn't so we must accept it in 4.3, since it does work
    CoreEvent event = flowRunner("flowWithErrorHandlerSelfReferencingToReferencedContinue").run();
    assertThat(event.getMessage().getPayload().getValue(), is("Lemon Pie"));
  }
}
