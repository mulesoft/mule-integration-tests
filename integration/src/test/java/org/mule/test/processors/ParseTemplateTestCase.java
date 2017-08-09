/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class ParseTemplateTestCase extends AbstractIntegrationTestCase {

  private static final String PARSED_NO_EXPRESSION = "This template does not have any expressions to parse";
  private static final String PARSED_MEL_EXPRESSION = "This template has a MEL expression to parse from mel-expression flow";
  private static final String PARSED_DW_EXPRESSION =
      "This template has a DW expression to parse from dw-expression flow. Remember, the name of the flow is dw-expression";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  public String getConfigFile() {
    return "org/mule/transformers/parse-template-config.xml";
  }


  @Test
  public void testNoExpressionInline() throws Exception {
    InternalEvent event = flowRunner("no-expression-inline").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionInline() throws Exception {
    InternalEvent event = flowRunner("mel-expression-inline").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionInline() throws Exception {
    InternalEvent event = flowRunner("dw-expression-inline").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testNoExpressionFromFile() throws Exception {
    InternalEvent event = flowRunner("no-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionFromFile() throws Exception {
    InternalEvent event = flowRunner("mel-expression").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionFromFile() throws Exception {
    InternalEvent event = flowRunner("dw-expression").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testNoExpressionFromLocation() throws Exception {
    InternalEvent event = flowRunner("no-expression-from-location").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionFromLocation() throws Exception {
    InternalEvent event = flowRunner("mel-expression-from-location").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionFromLocation() throws Exception {
    InternalEvent event = flowRunner("dw-expression-from-location").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testWithTargetDefaultTargetValueDefinedInline() throws Exception {
    String startingPayload = "Starting payload";
    InternalEvent event = flowRunner("with-target").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    String msg = (String) ((Message) event.getVariable("targetVar").getValue()).getPayload().getValue();
    String previousdPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
    assertEquals(previousdPayload, startingPayload);
  }

  @Test
  public void testWithTargetValueButNoTargetShouldRaiseException() throws Exception {
    expectedException.expect(MessagingException.class);
    flowRunner("with-target-value-no-target").withVariable("flowName", "dw-expression").run();
  }

  @Test
  public void testWithCustomTargetValue() throws Exception {
    String startingPayload = "Starting payload";
    InternalEvent event =
        flowRunner("with-custom-target-value").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    String savedPayload = (String) ((TypedValue) event.getVariable("targetVar").getValue()).getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, savedPayload);
    assertEquals(startingPayload, previousPayload);
  }

  @Test
  public void testWithWrongTargetValue() throws Exception {
    expectedException.expect(MessagingException.class);
    String startingPayload = "Starting payload";
    flowRunner("with-wrong-target-value").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
  }

  @Test
  public void testWithMessageBindingExpression() throws Exception {
    String startingPayload = "Starting payload";
    InternalEvent event =
        flowRunner("with-message-binding-target-value").withPayload(startingPayload).withVariable("flowName", "dw-expression")
            .run();
    TypedValue savedTypedValue = (TypedValue) event.getVariable("targetVar").getValue();
    assertEquals(savedTypedValue.getDataType().getType(), Message.class);
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, ((Message) savedTypedValue.getValue()).getPayload().getValue());
    assertEquals(startingPayload, previousPayload);
  }

  @Test
  public void testPayloadFromMessageBindingExpression() throws Exception {
    String startingPayload = "Starting payload";
    InternalEvent event =
        flowRunner("with-payload-from-message-binding-target-value").withPayload(startingPayload)
            .withVariable("flowName", "dw-expression").run();
    TypedValue savedTypedValue = (TypedValue) event.getVariable("targetVar").getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, savedTypedValue.getValue());
    assertEquals(startingPayload, previousPayload);
  }


}
