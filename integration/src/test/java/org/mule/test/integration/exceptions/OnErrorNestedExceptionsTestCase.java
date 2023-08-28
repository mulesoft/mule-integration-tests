/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("On Error Nested Exceptions")
public class OnErrorNestedExceptionsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-nested-exceptions-config.xml";
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToOuterWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndFails() throws Exception {
    flowRunner("propagatesToOuterWithoutExceptionAndFails")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated again"))));
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToFlowWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndFails() throws Exception {
    flowRunner("propagatesToFlowWithoutExceptionAndFails")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated again"))));
  }

  @Test
  public void propagatesToOuterWithExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToOuterWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithExceptionAndFails() throws Exception {
    flowRunner("propagatesToOuterWithExceptionAndFails")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated again"))));
  }

  @Test
  public void propagatesToFlowWithExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToFlowWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithExceptionAndFails() throws Exception {
    flowRunner("propagatesToFlowWithExceptionAndFails")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated again"))));
  }

  @Test
  public void exceptionInErrorHandlerFlowIsMarked() throws Exception {
    flowRunner("exceptionInErrorHandlerFlow")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated"))));
  }

  @Test
  public void exceptionInErrorHandlerTryIsMarked() throws Exception {
    flowRunner("exceptionInErrorHandlerTry")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated"))));
  }

  @Test
  public void exceptionInErrorHandlerNestedTryIsMarked() throws Exception {
    flowRunner("exceptionInErrorHandlerNestedTry")
        .runExpectingException(hasMessage(hasPayload(equalTo("propagated"))));
  }

  @Test
  public void exceptionInErrorHandlerNestedTryCorrectType() throws Exception {
    final CoreEvent event = flowRunner("exceptionInErrorHandlerNestedTryCorrectType").run();
    assertThat(event.getMessage(), hasPayload(equalTo("properly mapped")));
  }
}
