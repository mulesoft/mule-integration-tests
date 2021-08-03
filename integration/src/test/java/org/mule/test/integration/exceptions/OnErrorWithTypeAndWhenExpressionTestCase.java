/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

@Feature(ERROR_HANDLING)
@Story("When expression for error handler")
@RunnerDelegateTo(Parameterized.class)
public class OnErrorWithTypeAndWhenExpressionTestCase extends AbstractIntegrationTestCase {

  private String flowName;
  private boolean expectFailure;

  @Rule
  public ExpectedError expectedException = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-when-expression.xml";
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"withType", false},
        {"withIncorrectType", false},
        {"withWhen", false},
        {"withIncorrectWhen", false},
        {"withWhenAndType", false},
        {"withCorrectWhenIncorrectType", false},
        {"withIncorrectWhenCorrectType", false},
        {"withIncorrectWhenIncorrectType", false},
        {"defaultWhenConditionNotMet", true},
    });
  }

  public OnErrorWithTypeAndWhenExpressionTestCase(String flowName, boolean expectFailure) {
    this.flowName = flowName;
    this.expectFailure = expectFailure;
  }

  @Test
  public void runTest() throws Exception {
    if (expectFailure) {
      expectedException.expectMessage(containsString("An error occurred"));
    }
    CoreEvent event = flowRunner(flowName).run();
    assertThat(event.getMessage().getPayload().getValue(), is("Correct"));
  }


}
