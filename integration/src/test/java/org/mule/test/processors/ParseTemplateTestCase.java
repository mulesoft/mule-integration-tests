/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasMediaType;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.ParseTemplateStory.PARSE_TEMPLATE;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_16;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Story(PARSE_TEMPLATE)
public class ParseTemplateTestCase extends AbstractIntegrationTestCase {

  private static final String PARSED_NO_EXPRESSION = "This template does not have any expressions to parse";
  private static final String PARSED_DW_EXPRESSION =
      "This template has a DW expression to parse from dw-expression flow. Remember, the name of the flow is dw-expression";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  public String getConfigFile() {
    return "org/mule/processors/parse-template-config.xml";
  }


  @Test
  public void noExpressionInline() throws Exception {
    CoreEvent event = flowRunner("no-expression-inline").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void dwExpressionInline() throws Exception {
    CoreEvent event = flowRunner("dw-expression-inline").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void noExpressionFromFile() throws Exception {
    CoreEvent event = flowRunner("no-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void dwExpressionFromFile() throws Exception {
    CoreEvent event = flowRunner("dw-expression").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void noExpressionFromLocation() throws Exception {
    CoreEvent event = flowRunner("no-expression-from-location").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  public void dwExpressionFromLocation() throws Exception {
    CoreEvent event = flowRunner("dw-expression-from-location").withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
  }

  @Test
  public void withTargetDefaultTargetValueDefinedInline() throws Exception {
    String startingPayload = "Starting payload";
    CoreEvent event = flowRunner("with-target").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    String msg = (String) event.getVariables().get("targetVar").getValue();
    String previousdPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg);
    assertEquals(previousdPayload, startingPayload);
  }

  @Test
  public void withTargetValueButNoTargetShouldRaiseException() throws Exception {
    expectedException.expectCause(isA(IllegalArgumentException.class));
    flowRunner("with-target-value-no-target").withVariable("flowName", "dw-expression").run();
  }

  @Test
  public void withCustomTargetValue() throws Exception {
    String startingPayload = "Starting payload";
    CoreEvent event =
        flowRunner("with-custom-target-value").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    String savedPayload = (String) event.getVariables().get("targetVar").getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, savedPayload);
    assertEquals(startingPayload, previousPayload);
  }

  @Test
  public void withMessageBindingExpression() throws Exception {
    String startingPayload = "Starting payload";
    CoreEvent event =
        flowRunner("with-message-binding-target-value").withPayload(startingPayload).withVariable("flowName", "dw-expression")
            .run();
    Message savedTypedValue = (Message) event.getVariables().get("targetVar").getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertThat(savedTypedValue.getPayload().getValue(), is(PARSED_DW_EXPRESSION));
    assertThat(startingPayload, is(previousPayload));
  }

  @Test
  public void payloadFromMessageBindingExpression() throws Exception {
    String startingPayload = "Starting payload";
    CoreEvent event =
        flowRunner("with-payload-from-message-binding-target-value").withPayload(startingPayload)
            .withVariable("flowName", "dw-expression").run();
    String savedTypedValue = (String) event.getVariables().get("targetVar").getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertThat(savedTypedValue, is(PARSED_DW_EXPRESSION));
    assertThat(startingPayload, is(previousPayload));
  }

  @Test
  public void nestedExpressions() throws Exception {
    CoreEvent event = flowRunner("nestedExpressionsFlow").withVariable("individuals", "alpinos").withVariable("quantity", "3")
        .withVariable("origin", "war").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("They were 3 alpinos that came from war"));
  }

  @Test
  public void expressionEscaped() throws Exception {
    CoreEvent event = flowRunner("expressionEscaped").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("His name is #[pepito]"));
  }

  @Test
  public void nestedExpressionsWithNonexistingValues() throws Exception {
    expectedException.expectMessage("Unable to resolve reference of: `pepito`.");
    flowRunner("nestedExpressionsFlowWithNonexistentValues").run();
  }

  @Test
  public void nestedExpressionsAndQuote() throws Exception {
    CoreEvent event = flowRunner("nestedExpressionsAndQuoteFlow").withVariable("individuals", "alpinos")
        .withVariable("quantity", "3").withVariable("origin", "war").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("They weren't 3 alpinos that came from war"));
  }

  @Test
  public void nestedQuotedExpressionsAndQuoteFlow() throws Exception {
    CoreEvent event = flowRunner("nestedQuotedExpressionsAndQuoteFlow").withVariable("individuals", "alpinos")
        .withVariable("quantity", "3").withVariable("origin", "war").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("They weren't 3 alpinos that came from war"));
  }

  @Test
  public void nestedExpressionsFromFile() throws Exception {
    CoreEvent event = flowRunner("nestedExpressionsFlowFromFile").withVariable("chorusPhrase", "tiaitai rataplam").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("tiaitai rataplam, que venian de la guerra"));
  }

  @Test
  public void mimeTypeIsGuessed() throws Exception {
    CoreEvent event = flowRunner("jsonTemplateFromFile").withVariable("name", "El mismisimo Luciano Raineri Marchina").run();
    assertThat(event.getMessage(), hasMediaType(JSON));
  }

  @Test
  public void overriddenDataType() throws Exception {
    CoreEvent event = flowRunner("overriddenDataType").withVariable("flowName", "what do you care?").run();
    assertThat(event.getMessage(), hasMediaType(APPLICATION_JSON.withCharset(UTF_16)));
  }

  @Test
  public void overriddenEncodingFromMediaTypeParsing() throws Exception {
    CoreEvent event = flowRunner("overriddenEncodingFromMediaType").withVariable("flowName", "what do you care?").run();
    assertThat(event.getMessage(), hasMediaType(APPLICATION_JSON.withCharset(UTF_16)));
  }

  @Test
  public void encodingFromMediaTypeParsingIsReplacedIfSpecifiedInAttribute() throws Exception {
    CoreEvent event = flowRunner("encodingFromMediaTypeAndAttribute").withVariable("flowName", "what do you care?").run();
    assertThat(event.getMessage(), hasMediaType(APPLICATION_JSON.withCharset(UTF_16)));
  }

  @Test
  public void loadTemplateWithCustomEncoding() throws Exception {
    CoreEvent customEncodingEvent = flowRunner("loadWithCustomEncoding").run();
    CoreEvent defaultEncodingEvent = flowRunner("loadWithDefaultEncoding").run();
    assertThat(customEncodingEvent.getMessage().getPayload().getValue(),
               is(not(equalTo(defaultEncodingEvent.getMessage().getPayload()))));
  }

  @Test
  public void targetVariableAndValue() throws Exception {
    CoreEvent event = flowRunner("targetVariableAndValue").run();
    String msg = (String) event.getVariables().get("someVar").getValue();
    assertEquals(PARSED_NO_EXPRESSION, msg);
  }

  @Test
  @Issue("MULE-19900")
  public void nestedBackslash() throws Exception {
    CoreEvent event = flowRunner("nestedBackslash").withVariable("method", "GET").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("get:\\test\\GET"));
  }

  @Test
  public void subexpressionsExampleFromDocs() throws Exception {
    CoreEvent event = flowRunner("subexpressionsExampleFromDocs").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("<td>hello WORLD</td>\n"
        + "<td>hello WORLD</td>\n"
        + "<td>hello upper(\"world\")</td>\n"
        + "<td>hello ++ upper(\"world\")</td>"));
  }

  @Test
  public void escapeExampleFromDocs() throws Exception {
    CoreEvent event = flowRunner("escapeExampleFromDocs").run();

    // Current behavior here has [JKL] preceded by the escape character in the result which is not what we have in the docs
    assertThat(event.getMessage().getPayload().getValue(), equalTo("<td>#[</td>\n"
        + "<td>abcd#[-1234WORLD.log</td>\n"
        + "<td>'abc'def'</td>\n"
        + "<td>abc'def</td>\n"
        + "<td>\"xyz\"xyz\"</td>\n"
        + "<td>xyz\"xyz</td>\n"
        + "<td>abc$DEF#ghi\\[JKL]</td>"));
  }

  @Test
  @Issue("W-15141905")
  public void nestedExpression() throws Exception {
    String payload = "#[sum([1, 2, 3])]";
    CoreEvent event = flowRunner("nestedExpression").withPayload(payload).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("#[sum([1, 2, 3])]"));
  }

  @Test
  @Ignore("This case is not really supported at the moment. This test is just a remainder of that.")
  public void expressionWithinTransformation() throws Exception {
    CoreEvent event = flowRunner("expressionWithinTransformation").withPayload("world").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("uppercase payload is: WORLD"));
  }
}
