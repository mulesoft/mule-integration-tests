/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.DEFAULT_ERROR_HANDLER;

import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(DEFAULT_ERROR_HANDLER)
public class DefaultErrorHandlerTestCase extends AbstractIntegrationTestCase {

  private static Exception exception;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-config.xml";
  }

  @Test
  public void defaultCustomIsUsedWhenCatchAllMissingAndNoMatch() throws Exception {
    verifyWithException(new ExpressionRuntimeException(createStaticMessage("Error")), "defaultEH-custom");
  }

  @Test
  public void defaultAllIsUsedWhenCatchAllMissingAndNoMatch() throws Exception {
    verifyWithException(new RuntimeException("Error"), "defaultEH-all");
  }

  @Test
  public void flowIsUsedWhenCatchAllIsMissingButMatchFound() throws Exception {
    verifyWithException(new RetryPolicyExhaustedException(createStaticMessage("Error"),
                                                          mock(Initialisable.class,
                                                               withSettings().extraInterfaces(Component.class))),
                        "innerEH");
  }

  private void verifyWithException(Exception exceptionToThrow, String expectedPayload) throws Exception {
    exception = exceptionToThrow;
    assertThat(flowRunner("test").withPayload("").run().getMessage(), hasPayload(is(expectedPayload)));
  }

  protected static class ThrowExceptionProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw (MuleException) exception;
      }
    }
  }

}
