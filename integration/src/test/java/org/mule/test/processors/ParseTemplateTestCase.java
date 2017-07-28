/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

import org.junit.Test;


public class ParseTemplateTestCase extends AbstractIntegrationTestCase {

  private static final String PARSED_NO_EXPRESSION = "This template does not have any expressions to parse";
  private static final String PARSED_MEL_EXPRESSION = "This template has a MEL expression to parse from mel-expression flow";
  private static final String PARSED_DW_EXPRESSION =
      "This template has a DW expression to parse from dw-expression flow. Remember, the name of the flow is dw-expression";

  @Override
  public String getConfigFile() {
    return "org/mule/transformers/parse-template-config.xml";
  }


  @Test
  public void testNoExpressionInline() throws Exception {
    Event event = flowRunner("no-expression-inline").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionInline() throws Exception {
    Event event = flowRunner("mel-expression-inline").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionInline() throws Exception {
    Event event = flowRunner("dw-expression-inline").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testNoExpressionFromFile() throws Exception {
    Event event = flowRunner("no-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionFromFile() throws Exception {
    Event event = flowRunner("mel-expression").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionFromFile() throws Exception {
    Event event = flowRunner("dw-expression").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testNoExpressionFromLocation() throws Exception {
    Event event = flowRunner("no-expression-from-location").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void testMELExpressionFromLocation() throws Exception {
    Event event = flowRunner("mel-expression-from-location").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_MEL_EXPRESSION, msg);
  }

  @Test
  public void testDWExpressionFromLocation() throws Exception {
    Event event = flowRunner("dw-expression-from-location").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void testWithTargetDefinedInline() throws Exception {
    String startingPayload = "Starting payload";
    Event event = flowRunner("with-target").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    String msg = (String) ((Message) event.getVariable("targetVar").getValue()).getPayload().getValue();
    String processedPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
    assertEquals(processedPayload, startingPayload);
  }

}
