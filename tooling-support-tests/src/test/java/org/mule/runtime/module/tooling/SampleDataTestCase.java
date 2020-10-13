/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.CUSTOM_ERROR_CODE;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPWithAliasDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupWithOptionalProviderParamOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOptionalOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexActingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexParameterValue;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.errorSampleDataOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.innerPojo;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;
import static org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor.INVALID_PARAMETER_VALUE;
import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class SampleDataTestCase extends DeclarationSessionTestCase {

  @Test
  public void noSampleDataExposed() {
    String message = "Component multiLevelTypeKeyMetadataKey does not support Sample Data";
    OperationElementDeclaration elementDeclaration = multiLevelOPDeclaration(CONFIG_NAME, "America", "USA");
    assertSampleDataFailure(elementDeclaration, message, message, "NOT_SUPPORTED");
  }

  @Test
  public void configLessConnectionLessOperation() {
    assertSampleDataSuccess(configLessConnectionLessOPDeclaration(CONFIG_NAME), "Sample Data!", null);
  }

  @Test
  public void configLessConnectionLessOperationWithMissingConfigWorks() {
    assertSampleDataSuccess(configLessConnectionLessOPDeclaration(""), "Sample Data!", null);
  }

  @Test
  public void configLessOperation() {
    assertSampleDataSuccess(configLessOPDeclaration(CONFIG_NAME), "client", null);
  }

  @Test
  public void configLessOperationWithMissingConfigFails() {
    OperationElementDeclaration elementDeclaration = configLessOPDeclaration("");
    String message = "The sample data provider requires a connection and none was provided";
    String reason =
        "org.mule.sdk.api.data.sample.SampleDataException: The sample data provider requires a connection and none was provided\n";
    assertSampleDataFailure(elementDeclaration, message, reason, MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterOperation() {
    String actingParameter = "actingParameter";
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    assertSampleDataSuccess(elementDeclaration, null, actingParameter);
  }

  @Test
  public void actingParameterOptionalMissingOperationFails() {
    String actingParameter = "actingParameter";
    String message =
        "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [actingParameter]";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOptionalOPDeclaration(CONFIG_NAME);
    assertSampleDataFailure(elementDeclaration, message, reason, "MISSING_REQUIRED_PARAMETERS");
  }

  @Test
  public void complexActingParameterOperation() {
    int intParam = 0;
    String stringParam = "zero";
    List<String> listParam = asList("zero", "one", "two");
    Map<String, String> mapParam = ImmutableMap.of("0", "zero", "1", "one", "2", "two");
    ParameterValue innerPojoValue = innerPojo(intParam, stringParam, listParam, mapParam);
    List<ParameterValue> complexListParam = asList(innerPojoValue);
    Map<String, ParameterValue> complexMapParam = ImmutableMap.of("0", innerPojoValue, "1", innerPojoValue);
    ComponentElementDeclaration<?> elementDeclaration =
        complexActingParameterOPDeclaration(CONFIG_NAME,
                                            complexParameterValue(intParam, stringParam, listParam, mapParam, innerPojoValue,
                                                                  complexListParam, complexMapParam));
    String innerPojoStringValue = intParam +
        stringParam +
        "zeroonetwo" + //listParam
        "0zero1one2two"; //mapParam

    String expectedValue = intParam +
        stringParam +
        "zeroonetwo" + //listParam
        "0zero1one2two" + //mapParam
        innerPojoStringValue + //all inner pojo parameters
        innerPojoStringValue + //complex list with 1 inner pojo
        "0" + innerPojoStringValue + "1" + innerPojoStringValue; //complexMap

    assertSampleDataSuccess(elementDeclaration, null, expectedValue);
  }

  @Test
  public void actingParameterGroupOperation() {
    String stringValue = "stringValue";
    int intValue = 1;
    List<String> listValue = singletonList("single");

    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s-%s", stringValue, intValue, listValue.get(0)));
  }

  @Test
  public void actingParameterGroupMissingOptionalParamOperationFails() {
    int intValue = 1;
    List<String> listValue = singletonList("single");
    String message = "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [stringParam]";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";

    ComponentElementDeclaration<?> elementDeclaration = actingParameterGroupOPDeclaration(CONFIG_NAME, null, intValue, listValue);
    assertSampleDataFailure(elementDeclaration, message, reason, "MISSING_REQUIRED_PARAMETERS");
  }

  @Test
  public void actingParameterGroupMissingRequiredParamOperationFails() {
    String actingParameter = "actingParameter";
    List<String> listValue = singletonList("single");
    String message = "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [intParam]";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, actingParameter, null, listValue);
    assertSampleDataFailure(elementDeclaration, message, reason, "MISSING_REQUIRED_PARAMETERS");
  }

  @Test
  public void actingParameterMissingOperationFails() {
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, "");
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    String message =
        "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [actingParameter]";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    assertSampleDataFailure(elementDeclaration, message, reason, MISSING_REQUIRED_PARAMETERS);
  }

  @Test // TODO optional params does not appear in the model (MULE-18875)
  public void actingParameterGroupWithOptionalProviderParamOperation() {
    String stringValue = "stringValue";
    List<String> listValue = singletonList("single");
    Integer intProviderDefaultValue = 0;
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupWithOptionalProviderParamOPDeclaration(CONFIG_NAME, stringValue, null, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s-%s", stringValue, intProviderDefaultValue, listValue.get(0)));
  }

  @Test
  public void actingParameterGroupWithAliasOperation() {
    String stringValue = "stringValue";
    int intValue = 1;
    List<String> listValue = singletonList("single");
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPWithAliasDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s-%s", stringValue, intValue, listValue.get(0)));
  }

  @Test
  public void customErrorCodeFromProvider() {
    ComponentElementDeclaration<?> elementDeclaration = errorSampleDataOP(CONFIG_NAME);
    String message = "Expected error";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    assertSampleDataFailure(elementDeclaration, message, reason, CUSTOM_ERROR_CODE);
  }

  @Test
  public void actingParameterValueDefineWithExpression() {
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, "#[payload]");
    String message = "Error resolving value for parameter: 'actingParameter' from declaration, it cannot be an EXPRESSION value";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    assertSampleDataFailure(elementDeclaration, message, reason, INVALID_PARAMETER_VALUE);
  }

  @Test
  public void actingParameterConfigConnectionSource() {
    ComponentElementDeclaration<?> elementDeclaration = sourceDeclaration(CONFIG_NAME, "actingParameter");
    assertSampleDataSuccess(elementDeclaration, "client-actingParameter", "dummyConfig");
  }

  private void assertSampleDataSuccess(ComponentElementDeclaration<?> elementDeclaration, String expectedPayload,
                                       Object expectedAttributes) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(true));
    assertThat(sampleData.getSampleData().isPresent(), is(true));
    Message message = sampleData.getSampleData().get();

    assertThat(message.getPayload().getValue(), is(expectedPayload));
    assertThat(message.getAttributes().getValue(), is(expectedAttributes));
  }

  private void assertSampleDataFailure(ComponentElementDeclaration<?> elementDeclaration, String expectedMessage,
                                       String expectedReason, String expectedCode) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(false));
    assertThat(sampleData.getFailure().isPresent(), is(true));

    SampleDataFailure sampleDataFailure = sampleData.getFailure().get();
    assertThat(sampleDataFailure.getMessage(), is(expectedMessage));
    assertThat(sampleDataFailure.getReason(), is(expectedReason));
    assertThat(sampleDataFailure.getFailureCode(), is(expectedCode));
  }
}
