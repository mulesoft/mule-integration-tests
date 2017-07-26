/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ParseTemplateTestCase extends AbstractIntegrationTestCase {

  private static final String PARSED_NO_EXPRESSION = "This template does not have any expressions to parse";
  private static final String PARSED_MEL_EXPRESSION = "This template has a MEL expression to parse from mel-expression flow";
  private static final String PARSED_DW_EXPRESSION = "This template has a DW expression to parse from dw-expression flow";

  @Override
  public String getConfigFile() {
    return "org/mule/transformers/parse-template-config.xml";
  }


  @Test
  public void testNoExpression() throws Exception {
    Event event = flowRunner("no-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(msg, PARSED_NO_EXPRESSION);
  }

  @Test
  public void testMELExpression() throws Exception {
    Event event = flowRunner("mel-expression").withVariable("flowName", "mel-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(msg, PARSED_MEL_EXPRESSION);
  }

  @Test
  public void testDWExpression() throws Exception {
    Event event = flowRunner("dw-expression").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(msg, PARSED_DW_EXPRESSION);
  }



}
