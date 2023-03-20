/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class ExceptionStrategyCommonScenariosTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE_TO_SEND = "A message";
  public static final String MESSAGE_MODIFIED = "A message with some text added";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-flow.xml";
  }

  @Test
  public void testPreservePayloadPropagate() throws Exception {
    flowRunner("PreservePayloadPropagate").withPayload(MESSAGE_TO_SEND)
        .runExpectingException(errorType("APP", "EXPECTED"), hasMessage(hasPayload(is(MESSAGE_MODIFIED))));
  }

  @Test
  public void testPreservePayloadContinue() throws Exception {
    final CoreEvent result = flowRunner("PreservePayloadContinue").withPayload(MESSAGE_TO_SEND).run();
    assertThat(result, hasMessage(hasPayload(is(MESSAGE_MODIFIED))));
  }

}
