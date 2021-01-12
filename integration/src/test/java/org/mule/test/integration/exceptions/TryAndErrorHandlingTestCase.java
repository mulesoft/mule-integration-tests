/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mule.functional.api.component.InvocationCountMessageProcessor.getNumberOfInvocationsFor;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class TryAndErrorHandlingTestCase extends AbstractIntegrationTestCase {

  private TestConnectorQueueHandler queueHandler;

  @Before
  public void before() {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

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
    Message response = queueHandler.read("dlq", RECEIVE_TIMEOUT).getMessage();
    assertThat(response, notNullValue());
  }

  @Test
  public void tryWithRecursiveOnErrorContinueInsideFlow() throws Exception {
    flowRunner("tryWithRecursiveOnErrorContinueInsideFlow").run();
    Message response = queueHandler.read("dlq", RECEIVE_TIMEOUT).getMessage();
    assertThat(response, notNullValue());
  }

}
