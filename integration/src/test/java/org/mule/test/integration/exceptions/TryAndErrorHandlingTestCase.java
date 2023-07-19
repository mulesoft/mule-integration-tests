/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.mule.functional.api.component.InvocationCountMessageProcessor.getNumberOfInvocationsFor;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tests.api.TestQueueManager;

import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class TryAndErrorHandlingTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/try-error-handling.xml";
  }

  @Test
  @Description("Validates that try scope works within an error handler")
  public void tryInErrorHandler() throws Exception {
    CoreEvent event = flowRunner("tryInErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
    assertThat(getNumberOfInvocationsFor("try-in-eh"), is(0));
  }

  @Test
  public void tryWithRecursiveOnErrorContinueInsideSubflow() throws Exception {
    flowRunner("tryWithRecursiveOnErrorContinueInsideSubflow").run();
    Message response = queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(response, notNullValue());
  }

  @Test
  public void tryWithRecursiveOnErrorContinueInsideFlow() throws Exception {
    flowRunner("tryWithRecursiveOnErrorContinueInsideFlow").run();
    Message response = queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertThat(response, notNullValue());
  }

  @Test
  @Issue("W-11861131")
  public void tryWithOnErrorHandlersComposition() throws Exception {
    flowRunner("tryWithOnErrorHandlersComposition").run();
  }

}
