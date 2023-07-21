/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ON_ERROR_CONTINUE;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ON_ERROR_CONTINUE)
public class ExceptionStrategyReturnMessageTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
  }

  @Test
  public void testReturnPayloadDefaultStrategy() throws Exception {
    flowRunner("InputService2").withPayload("Test Message")
        .runExpectingException(errorType("APP", "EXPECTED"), hasMessage(hasPayload(not(nullValue(String.class)))));
  }

  @Test
  public void testReturnPayloadCustomStrategy() throws Exception {
    CoreEvent event = flowRunner("InputService").withPayload("Test Message").run();
    Message msg = event.getMessage();

    assertNotNull(msg);

    assertNotNull(msg.getPayload().getValue());
    assertEquals("Ka-boom!", msg.getPayload().getValue());
  }

}
