/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.UnauthorisedException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.test.AbstractIntegrationTestCase;

import java.sql.SQLDataException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.hamcrest.Matchers;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("Error Handler")
public class ErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final I18nMessage mockMessage = mock(I18nMessage.class);

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler.xml";
  }

  @Test
  public void testMatchesCorrectExceptionStrategyUsingGroovyExpressionEvaluator() throws Exception {
    callAndThrowException("groovy", new SQLDataException(), "groovy catch-6");
  }

  @Test
  public void transformation() throws Exception {
    String expectedMessage = "0 transformation";
    Transformer mockTransformer = mock(Transformer.class);
    callTypeAndThrowException(new TransformerException(mockMessage, mockTransformer), expectedMessage);
  }

  @Test
  public void expression() throws Exception {
    callTypeAndThrowException(new ExpressionRuntimeException(mockMessage, new Exception()), "0 expression");
  }

  @Test
  public void connectivity() throws Exception {
    callTypeAndThrowException(new RetryPolicyExhaustedException(mockMessage,
                                                                mock(Initialisable.class,
                                                                     withSettings().extraInterfaces(Component.class))),
                              "0 connectivity");
  }

  @Test
  public void security() throws Exception {
    callTypeAndThrowException(new UnauthorisedException(mockMessage), "0 security");
  }

  @Test
  public void any() throws Exception {
    String expectedMessage = "0 any";
    callTypeAndThrowException(new RuntimeException(), expectedMessage);
    callTypeAndThrowException(new DefaultMuleException(mockMessage), expectedMessage);
  }

  @Test
  public void criticalNotHandled() throws Exception {
    flowRunner("propagatesCriticalErrors").withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType(Matchers.any(String.class), is("CRITICAL")));
  }

  @Test
  public void innerRoutingErrorPropagated() throws Exception {
    flowRunner("onErrorFails").withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("MULE", "EXPRESSION"));
  }

  @Test
  public void innerRoutingErrorHandled() throws Exception {
    assertThat(flowRunner("propagatesErrorHandlingRoutingErrors").withPayload(TEST_PAYLOAD).run().getMessage(),
               hasPayload(equalTo(TEST_PAYLOAD + " expression")));
  }

  private void callTypeAndThrowException(Exception exception, String expectedMessage) throws Exception {
    Message response = flowRunner("matchesHandlerUsingType")
        .withPayload("0")
        .withVariable("exception", exception)
        .run()
        .getMessage();
    assertThat(getPayloadAsString(response), is(expectedMessage));
  }

  private void callAndThrowException(Object payload, final Exception exceptionToThrow, final String expectedMessage)
      throws Exception {
    getFromFlow(locator, "matchesHandlerUsingWhen").setEventCallback((context, component, muleContext) -> {
      throw exceptionToThrow;
    });
    Message response =
        flowRunner("matchesHandlerUsingWhen").withPayload(payload).run().getMessage();
    assertThat(getPayloadAsString(response), is(expectedMessage));
  }

  public static class BaseException extends Exception {
  }

  public static class SubtypeException extends BaseException {
  }

  public static class SubtypeSubtypeException extends SubtypeException {
  }

  public static class AnotherTypeMyException extends Exception {
  }

  public static class StartsWithException extends Exception {
  }

  public static class ThisExceptionFinishesWithException extends Exception {
  }

  public static class ThisExceptionFinishesWithSomethingElse extends Exception {
  }

  public static class AnotherTotallyDifferentKindOfException extends Exception {
  }

  public static class GenericMuleException extends MuleException {

    public GenericMuleException(I18nMessage message) {
      super(message);
    }

    public GenericMuleException(I18nMessage message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class ThrowErrorProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new AssertionError("validation failed");
    }

  }
}
