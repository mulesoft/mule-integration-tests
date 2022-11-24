/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

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
  public void lookupCallsFlow() throws Exception {
    assertThat(flowRunner("staticParams").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo(API_RESPONSE)));
  }

  @Test
  public void lookupUsesJavaTypeRegardlessOfMessageType() throws Exception {
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
  public void lookupCallsFlowThroughExpressions() throws Exception {
    assertThat(flowRunner("expressionParams")
        .withVariable("flow", "callApi")
        .withPayload(TEST_PAYLOAD)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo(API_RESPONSE)));
  }

  @Test
  public void lookupResultCanBeManipulated() throws Exception {
    assertThat(flowRunner("composition").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("Ana from BA")));
  }

  @Test
  @Description("Verifies that variables, attributes and errors are propagated forward, the established payload used and that the result payload is propagated back including it's type.")
  public void lookupDataIsPropagatedInBothDirections() throws Exception {
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
  public void lookupCallsFlowThatHandlesError() throws Exception {
    assertThat(flowRunner("expressionParams")
        .withVariable("flow", "failureHandledFlow")
        .withPayload(TEST_PAYLOAD)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo("Flow failed but lets move on")));
  }

  @Test
  public void lookupFailsWhenCalledFlowThrowsError() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(isA(ExpressionRuntimeException.class));
    expectedError.expectMessage(containsString("Flow 'failingFlow' has failed with error 'APP:EXPECTED' (expected error)"));
    flowRunner("expressionParams")
        .withVariable("flow", "failingFlow")
        .withPayload(TEST_PAYLOAD)
        .run();
  }

  @Test
  public void lookupFailsWhenCalledFlowTimesOut() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(isA(ExpressionRuntimeException.class));
    expectedError.expectMessage(containsString("Flow 'timeoutFlow' has timed out after 100 millis"));

    flowRunner("expressionParamsWithTimeout")
        .withVariable("flow", "timeoutFlow")
        .withPayload(TEST_PAYLOAD)
        .run();
  }

  @Test
  public void lookupCallsFlowThatHandlesConnectorError() throws Exception {
    assertThat(flowRunner("staticParams")
        .withVariable("status", SC_UNAUTHORIZED)
        .keepStreamsOpen()
        .run()
        .getMessage(),
               hasPayload(equalTo("Request was unauthorized but lets move on")));
  }

  @Test
  public void lookupFailsWhenCalledFlowThrowsConnectorError() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(isA(ExpressionRuntimeException.class));
    expectedError
        .expectMessage(containsString(format("\"Exception while executing lookup(\"callApi\",\"data\",2000 as Number {class: \"java.lang.Integer\"}) cause: Flow 'callApi' has failed "
            + "with error 'HTTP:METHOD_NOT_ALLOWED' (HTTP GET on resource 'http://localhost:%s/405' "
            + "failed: method not allowed (405).) \n", port.getValue())));
    flowRunner("staticParams").withVariable("status", SC_METHOD_NOT_ALLOWED).run();
  }

  @Test
  public void lookupFailsWhenFlowDoesNotExist() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(isA(ExpressionRuntimeException.class));
    expectedError.expectMessage(containsString("There is no component named 'non-existent'."));
    flowRunner("expressionParams").withVariable("flow", "non-existent").withPayload(TEST_PAYLOAD).run();
  }

  @Test
  public void lookupFailsWhenReferenceIsNotAFlow() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectCause(isA(ExpressionRuntimeException.class));
    expectedError.expectMessage(containsString("Component 'request-config' is not a flow."));
    flowRunner("expressionParams").withVariable("flow", "request-config").withPayload(TEST_PAYLOAD).run();
  }

  @Test
  public void accessObjectsFromRegistryBinding() throws Exception {
    flowRunner("registryBindingFlow").run();
  }

  @Test
  @Ignore("W-12117759")
  public void checkCompatibleDataTypes() throws Exception {
    DataType compatible1 = DataType.builder().type(Parent.class).mediaType(MediaType.ANY).build();
    DataType compatible2 = DataType.builder().type(Child.class).mediaType(MediaType.APPLICATION_JAVA).build();
    DataType nonCompatible = DataType.STRING;
    flowRunner("checkCompatibleDataTypes").withVariable("compatible1", compatible1).withVariable("compatible2", compatible2)
        .withVariable("nonCompatible", nonCompatible).run().getMessage();
  }

  @Test
  public void causedBySameType() throws Exception {
    assertThat(flowRunner("sameType").run().getMessage(), hasPayload(equalTo("A connection failed.")));
  }

  @Test
  public void causedByParentType() throws Exception {
    assertThat(flowRunner("subType").run().getMessage(), hasPayload(equalTo("A connection failed.")));
  }

  @Test
  public void causedByNonExistingType() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectMessage(containsString("Could not find error type 'ZARAZA'"));
    flowRunner("nonExistentType").run();
  }

  @Test
  public void causedByNullType() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectMessage(containsString("identifier cannot be an empty string or null"));
    flowRunner("nullType").run();
  }

  @Test
  public void causedByWithoutError() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    expectedError.expectMessage(containsString("There's no error to match against"));
    flowRunner("noError").run();
  }

  private static class Parent {

  }

  private static class Child extends Parent {

  }

  public static String sleepForTimeoutTest() {
    // just calling sleep from DW causes the thrown InterruptedException to be unhandled by DW and bubble up.
    try {
      sleep(10000L);
      return "Good Morning!";
    } catch (InterruptedException e) {
      currentThread().interrupt();
      return "Rude Awakening";
    }

  }
}
