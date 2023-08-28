/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.RAISE_ERROR;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.LinkedList;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.Test;

@Epic(ERROR_HANDLING)
@Feature(RAISE_ERROR)
public class RaiseErrorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "raise-error-config.xml";
  }

  @Test
  public void raisesNewErrorType() throws Exception {
    expectedError.expectMessage(is("An error occurred."));
    expectedError.expectErrorType("APP", "MY_TYPE");
    flowRunner("customError").run();
  }

  @Test
  public void raisesExistingErrorType() throws Exception {
    expectedError.expectMessage(is("An error occurred."));
    expectedError.expectErrorType("MULE", "CONNECTIVITY");
    flowRunner("existingError").run();
  }

  @Test
  public void handlesStringDescription() throws Exception {
    expectedError.expectMessage(is("This is a routing error."));
    expectedError.expectErrorType("MULE", "ROUTING");
    flowRunner("descriptionString").run();
  }

  @Test
  public void handlesExpressionDescription() throws Exception {
    expectedError.expectMessage(is("The error was caused by: test"));
    expectedError.expectErrorType("APP", "MY_TYPE");
    flowRunner("descriptionExpression").withPayload(TEST_PAYLOAD).withMediaType(TEXT).run();
  }

  @Test
  public void overridesContinue() throws Exception {
    expectedError.expectMessage(is("An error occurred."));
    expectedError.expectErrorType("APP", "MY_TYPE");
    flowRunner("continueOverride").run();
  }

  @Test
  public void overridesPropagatedError() throws Exception {
    expectedError.expectMessage(is("An error occurred."));
    expectedError.expectErrorType("APP", "MY_TYPE");
    flowRunner("propagateOverride").run();
  }

  @Test
  public void canBeHandled() throws Exception {
    assertThat(flowRunner("handled").keepStreamsOpen().run().getMessage(), hasPayload(equalTo("Fatal error.")));
  }

  @Test
  public void withinTryHandle() throws Exception {
    assertThat(flowRunner("try").withVariable("continue", true).keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("Try error was handled.")));
  }

  @Test
  public void withinTryPropagate() throws Exception {
    assertThat(flowRunner("try").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("Try error was propagated.")));
  }

  @Test
  public void withinForEach() throws Exception {
    LinkedList<Object> payload = new LinkedList<>();
    payload.add(TEST_PAYLOAD);
    assertThat(flowRunner("foreach").withPayload(payload).keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("For each failed.")));
  }

  @Test
  public void withinScatterGather() throws Exception {
    assertThat(flowRunner("scatter").withPayload(TEST_PAYLOAD).keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("Scatter gather route failed.")));
  }

  @Test
  public void withinForEachAndTryScope() throws Exception {
    // If error is properly catch, nothing should fail.
    assertThat(flowRunner("tryAndForEach").run().getMessage(), hasPayload(equalTo("Executed OnErrorContinue")));
  }

}
