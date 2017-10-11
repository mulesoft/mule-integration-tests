/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

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
        .runExpectingException(instanceOf(FunctionalTestException.class), hasMessage(hasPayload(is(MESSAGE_MODIFIED))));
  }

  @Test
  public void testPreservePayloadContinue() throws Exception {
    final CoreEvent result = flowRunner("PreservePayloadContinue").withPayload(MESSAGE_TO_SEND).run();
    assertThat(result, hasMessage(hasPayload(is(MESSAGE_MODIFIED))));
  }

}
