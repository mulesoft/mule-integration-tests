/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;

import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class ExceptionStrategyCommonScenariosTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE_TO_SEND = "A message";
  public static final String MESSAGE_MODIFIED = "A message with some text added";
  public static final int TIMEOUT = 5000;

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Rule
  public DynamicPort dynamicPort4 = new DynamicPort("port4");

  @Rule
  public DynamicPort dynamicPort5 = new DynamicPort("port5");

  @Rule
  public DynamicPort dynamicPort6 = new DynamicPort("port6");


  @Rule
  public DynamicPort dynamicPort7 = new DynamicPort("port7");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-common-scenarios-flow.xml";
  }

  @Test
  public void testPreservePayloadExceptionStrategy() throws Exception {
    flowRunner("PreservePayloadExceptionStrategy").withPayload(MESSAGE_TO_SEND)
        .runExpectingException(instanceOf(FunctionalTestException.class), hasMessage(hasPayload(is(MESSAGE_MODIFIED))));
  }

}
