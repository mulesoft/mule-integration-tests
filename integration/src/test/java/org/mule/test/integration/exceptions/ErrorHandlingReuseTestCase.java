/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.security.UnauthorisedException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story("Error Handling Reuse")
public class ErrorHandlingReuseTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handling-reuse-config.xml";
  }

  @Test
  public void usesReferencedErrorHandler() throws Exception {
    verifyFlowWhenThrowing("withSharedHandler",
                           new UnauthorisedException(createStaticMessage("Can't touch this!")),
                           " security");
  }

  @Test
  public void usesReferencedErrorHandlerWithGeneralOnErrorPropagateReference() throws Exception {
    expectedError.expectErrorType("MULE", "UNKNOWN");
    expectedError.expectEvent(hasMessage(hasPayload(equalTo("test any shared"))));
    flowRunner("withSharedHandler")
        .withPayload(TEST_PAYLOAD)
        .withVariable("exception", new IndexOutOfBoundsException())
        .run();
  }

  @Test
  public void usesReferencedErrorHandlerWithMidOnErrorContinueReference() throws Exception {
    verifyFlowWhenThrowing("withSharedHandler",
                           new ExpressionRuntimeException(createStaticMessage("Failed")),
                           " expression");
  }

  @Test
  public void usesReferencedOnError() throws Exception {
    verifyFlowWhenThrowing("withSharedHandlersInline",
                           new ExpressionRuntimeException(createStaticMessage("Failed")),
                           " expression");
  }

  @Test
  public void goesThroughReferencedOnError() throws Exception {
    verifyFlowWhenThrowing("withSharedHandlersInline",
                           new RoutingException(createStaticMessage("Wrong turn"), mock(Processor.class)),
                           " routing");
  }

  @Test
  public void usesReferencedErrorHandlerInTry() throws Exception {
    verifyFlowWhenThrowing("withTryAndSharedHandler",
                           new RoutingException(createStaticMessage("Wrong turn"), mock(Processor.class)),
                           " hey routing");
  }

  @Test
  public void usesReferencedOnErrorInTry() throws Exception {
    verifyFlowWhenThrowing("withTryAndSharedHandlersInline",
                           new ExpressionRuntimeException(createStaticMessage("Oh, man!")),
                           " hey expression");
  }

  private void verifyFlowWhenThrowing(String flowName, Exception exception, String expectedAppend) throws Exception {
    assertThat(flowRunner(flowName).withPayload(TEST_PAYLOAD).withVariable("exception", exception).run().getMessage(),
               hasPayload(equalTo(TEST_PAYLOAD + expectedAppend)));
  }

}
