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
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class ExpressionsOnErrorsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/expressions-on-errors.xml";
  }

  @Test
  public void detailedDescription() throws Exception {
    assertThat(flowRunner("detailedDescription").run().getMessage().getPayload().getValue(),
               is("An expected error occurred."));
  }

  @Test
  public void infoElement() throws Exception {
    assertThat(flowRunner("infoElement").run().getMessage().getPayload().getValue(),
               is("infoElement/processors/0 @ ExpressionsOnErrorsTestCase#infoElement:org/mule/test/integration/exceptions/expressions-on-errors.xml:16"));
  }

  @Test
  @Issue("MULE-18535")
  public void infoElementDeprecated() throws Exception {
    assertThat(flowRunner("infoElementDeprecated").run().getMessage().getPayload().getValue(),
               is("infoElementDeprecated/processors/0 @ ExpressionsOnErrorsTestCase#infoElementDeprecated:org/mule/test/integration/exceptions/expressions-on-errors.xml:25"));
  }
}
