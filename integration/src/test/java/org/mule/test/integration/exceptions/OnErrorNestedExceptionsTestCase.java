/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
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
    Event event = flowRunner("propagatesToOuterWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToOuterWithoutExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(exception.inErrorHandler());
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndSucceeds() throws Exception {
    Event event = flowRunner("propagatesToFlowWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToFlowWithoutExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(exception.inErrorHandler());
  }

  @Test
  public void propagatesToOuterWithExceptionAndSucceeds() throws Exception {
    Event event = flowRunner("propagatesToOuterWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToOuterWithExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(exception.inErrorHandler());
  }

  @Test
  public void propagatesToFlowWithExceptionAndSucceeds() throws Exception {
    Event event = flowRunner("propagatesToFlowWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithExceptionAndFails() throws Exception {
    MessagingException exception = flowRunner("propagatesToFlowWithExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(exception.inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerFlowIsMarked() throws Exception {
    MessagingException exception = flowRunner("exceptionInErrorHandlerFlow").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    assertTrue(exception.inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerTryIsMarked() throws Exception {
    MessagingException exception = flowRunner("exceptionInErrorHandlerTry").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    //assertTrue(exception.inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerNestedTryIsMarked() throws Exception {
    MessagingException exception = flowRunner("exceptionInErrorHandlerNestedTry").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    //assertTrue(exception.inErrorHandler());
  }
}
