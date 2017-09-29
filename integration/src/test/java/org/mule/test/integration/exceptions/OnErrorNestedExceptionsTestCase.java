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

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.EventProcessingException;
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
    CoreEvent event = flowRunner("propagatesToOuterWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithoutExceptionAndFails() throws Exception {
    EventProcessingException exception = flowRunner("propagatesToOuterWithoutExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(((MessagingException) exception).inErrorHandler());
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToFlowWithoutExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithoutExceptionAndFails() throws Exception {
    EventProcessingException exception = flowRunner("propagatesToFlowWithoutExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(((MessagingException) exception).inErrorHandler());
  }

  @Test
  public void propagatesToOuterWithExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToOuterWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToOuterWithExceptionAndFails() throws Exception {
    EventProcessingException exception = flowRunner("propagatesToOuterWithExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(((MessagingException) exception).inErrorHandler());
  }

  @Test
  public void propagatesToFlowWithExceptionAndSucceeds() throws Exception {
    CoreEvent event = flowRunner("propagatesToFlowWithExceptionAndSucceeds").run();
    assertThat(event.getMessage(), hasPayload(equalTo("propagated again")));
  }

  @Test
  public void propagatesToFlowWithExceptionAndFails() throws Exception {
    EventProcessingException exception = flowRunner("propagatesToFlowWithExceptionAndFails").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated again")));
    assertFalse(((MessagingException) exception).inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerFlowIsMarked() throws Exception {
    EventProcessingException exception = flowRunner("exceptionInErrorHandlerFlow").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    assertTrue(((MessagingException) exception).inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerTryIsMarked() throws Exception {
    EventProcessingException exception = flowRunner("exceptionInErrorHandlerTry").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    //assertTrue(exception.inErrorHandler());
  }

  @Test
  public void exceptionInErrorHandlerNestedTryIsMarked() throws Exception {
    EventProcessingException exception = flowRunner("exceptionInErrorHandlerNestedTry").runExpectingException();
    assertThat(exception.getEvent().getMessage(), hasPayload(equalTo("propagated")));
    //assertTrue(exception.inErrorHandler());
  }
}
