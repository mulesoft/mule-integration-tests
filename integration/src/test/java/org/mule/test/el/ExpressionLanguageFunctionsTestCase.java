/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_FUNCTIONS;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_FUNCTIONS)
public class ExpressionLanguageFunctionsTestCase extends AbstractIntegrationTestCase {

  public static final String API_RESPONSE = "{\n  \"name\": \"Ana\",\n  \"location\": \"BA\"\n}";
  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-functions-config.xml";
  }

  @Test
  public void callsFlow() throws Exception {
    assertThat(flowRunner("staticParams").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo(API_RESPONSE)));
  }

  @Test
  public void usesJavaTypeRegardlessOfMessageType() throws Exception {
    TypedValue result = flowRunner("complex")
        .withPayload("{\"hey\" : \"there\"}")
        .withMediaType(JSON)
        .keepStreamsOpen()
        .run().getMessage().getPayload();

    Collection<String> value = (Collection<String>) result.getValue();
    assertThat(value, hasItems("oh", "there"));
    assertThat(result.getDataType().getMediaType(), is(APPLICATION_JAVA.withCharset(UTF_8)));
  }

  @Test
  public void callsFlowThroughExpressions() throws Exception {
    assertThat(flowRunner("expressionParams")
        .withVariable("flow", "callApi")
        .withPayload(TEST_PAYLOAD)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo(API_RESPONSE)));
  }

  @Test
  public void resultCanBeManipulated() throws Exception {
    assertThat(flowRunner("composition").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("Ana from BA")));
  }

  @Test
  @Description("Verifies that variables, attributes and errors are propagated forward, the established payload used and that the result payload is propagated back including it's type.")
  public void dataIsPropagatedInBothDirections() throws Exception {
    Map<String, String> payload = new HashMap<>();
    payload.put("key", "value");

    Message result = flowRunner("fromErrorHandler")
        .withPayload(payload)
        .withVariable("text", "some text", TEXT_STRING)
        .withAttributes(new String[] {"first", "second"})
        .keepStreamsOpen().runAndVerify("propagation").getMessage();

    assertThat(result, hasPayload(equalTo("Propagation was successful")));
    assertThat(result.getPayload().getDataType().getMediaType().toRfcString(), is("text/plain; charset=UTF-8"));
  }

  @Test
  public void callsFlowThatHandlesError() throws Exception {
    assertThat(flowRunner("expressionParams")
        .withVariable("flow", "failureHandledFlow")
        .withPayload(TEST_PAYLOAD)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo("Flow failed but lets move on")));
  }

  @Test
  public void failsWhenCalledFlowThrowsError() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(both(isA(ExpressionRuntimeException.class))
        .and(hasMessage(equalTo(("\"Exception while executing lookup(\"failingFlow\" as String {class: \"java.lang.String\", "
            + "encoding: \"UTF-8\", mimeType: \"*/*\"},\"test\" as String {class: \"java.lang.String\", encoding: \"UTF-8\", "
            + "mimeType: \"*/*\"}) cause: Flow 'failingFlow' has failed with error 'MULE:UNKNOWN' (Functional Test Service Exception) \n"
            + "\n"
            + "Trace:\n"
            + "  at 'lookup' in (anonymous:1:1)\n"
            + "  at 'main'   in (anonymous:1:1)\" evaluating expression: \"lookup(vars.flow, payload)\".")))));
    flowRunner("expressionParams")
        .withVariable("flow", "failingFlow")
        .withPayload(TEST_PAYLOAD)
        .run();
  }

  @Test
  public void callsFlowThatHandlesConnectorError() throws Exception {
    assertThat(flowRunner("staticParams")
        .withVariable("status", SC_UNAUTHORIZED)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo("Request was unauthorized but lets move on")));
  }

  @Test
  public void failsWhenCalledFlowThrowsConnectorError() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(both(isA(ExpressionRuntimeException.class))
        .and(hasMessage(equalTo(format("\"Exception while executing lookup(\"callApi\",\"data\") cause: Flow 'callApi' has failed "
            + "with error 'HTTP:METHOD_NOT_ALLOWED' (HTTP GET on resource 'http://localhost:%s/405' "
            + "failed: method not allowed (405).) \n"
            + "\n"
            + "Trace:\n"
            + "  at 'lookup' in (anonymous:1:1)\n"
            + "  at 'main'   in (anonymous:1:1)\" evaluating expression: \"lookup('callApi', 'data')\".", port.getValue())))));
    flowRunner("staticParams").withVariable("status", SC_METHOD_NOT_ALLOWED).run();
  }

  @Test
  public void failsWhenFlowDoesNotExist() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(both(isA(ExpressionRuntimeException.class))
        .and(hasMessage(equalTo(("\"Exception while executing lookup(\"non-existent\" as String {class: \"java.lang.String\", "
            + "encoding: \"UTF-8\", mimeType: \"*/*\"},\"test\" as String {class: \"java.lang.String\", encoding: \"UTF-8\", "
            + "mimeType: \"*/*\"}) cause: There is no component named 'non-existent'. \n"
            + "\n"
            + "Trace:\n"
            + "  at 'lookup' in (anonymous:1:1)\n"
            + "  at 'main'   in (anonymous:1:1)\" evaluating expression: \"lookup(vars.flow, payload)\".")))));
    flowRunner("expressionParams").withVariable("flow", "non-existent").withPayload(TEST_PAYLOAD).run();
  }

  @Test
  public void accessObjectsFromRegistryBinding() throws Exception {
    flowRunner("registryBindingFlow").run();
  }

  @Test
  public void failsWhenReferenceIsNotAFlow() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(both(isA(ExpressionRuntimeException.class))
        .and(hasMessage(equalTo(("\"Exception while executing lookup(\"request-config\" as String {class: \"java.lang.String\", "
            + "encoding: \"UTF-8\", mimeType: \"*/*\"},\"test\" as String {class: \"java.lang.String\", encoding: \"UTF-8\", "
            + "mimeType: \"*/*\"}) cause: Component 'request-config' is not a flow. \n"
            + "\n"
            + "Trace:\n"
            + "  at 'lookup' in (anonymous:1:1)\n"
            + "  at 'main'   in (anonymous:1:1)\" evaluating expression: \"lookup(vars.flow, payload)\".")))));
    flowRunner("expressionParams").withVariable("flow", "request-config").withPayload(TEST_PAYLOAD).run();
  }

  @Test
  public void checkCompatibleDataTypes() throws Exception {
    DataType compatible1 = DataType.builder().type(Parent.class).mediaType(MediaType.ANY).build();
    DataType compatible2 = DataType.builder().type(Child.class).mediaType(MediaType.APPLICATION_JAVA).build();
    DataType nonCompatible = DataType.STRING;
    flowRunner("checkCompatibleDataTypes").withVariable("compatible1", compatible1).withVariable("compatible2", compatible2)
        .withVariable("nonCompatible", nonCompatible).run().getMessage();
  }

  private static class Parent {

  }

  private static class Child extends Parent {

  }

}
