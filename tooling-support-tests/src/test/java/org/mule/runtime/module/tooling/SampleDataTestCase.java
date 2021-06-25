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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.CUSTOM_ERROR_CODE;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPWithAliasDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupWithOptionalWithDefaultInContainerProviderParamOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOptionalWithoutDefaultOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexActingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexParameterValue;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.errorSampleDataOP;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.innerPojo;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnAttributeOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnDeepFieldAsPojoOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnDeepFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpWithBindingToTopLevelOPDeclaration;
import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOptionalWithoutDefaultOPDeclaration(CONFIG_NAME, null);
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
        "zeroonetwo" + // listParam
        "0zero1one2two"; // mapParam

    String expectedValue = intParam +
        stringParam +
        "zeroonetwo" + // listParam
        "0zero1one2two" + // mapParam
        innerPojoStringValue + // all inner pojo parameters
        innerPojoStringValue + // complex list with 1 inner pojo
        "0" + innerPojoStringValue + "1" + innerPojoStringValue; // complexMap

    assertSampleDataSuccess(elementDeclaration, null, expectedValue);
  }

  @Test
  public void actingParameterGroupOperation() {
    String stringValue = "stringValue";
    int intValue = 1;
    List<String> listValue = singletonList("single");

    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s--100-%s", stringValue, intValue, listValue.get(0)));
  }

  @Test
  public void actingParameterGroupOptionalParamWithDefaultOperation() {
    int intValue = 1;
    List<String> listValue = singletonList("single");
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, null, intValue, null, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s-%s-%s", "defaultStringValue", "1", "-100", listValue.get(0)));
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

  @Test
  public void actingParameterGroupWithOptionalProviderParamOperation() {
    List<String> listValue = singletonList("single");
    Integer intProviderDefaultValue = 0;
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupWithOptionalWithDefaultInContainerProviderParamOPDeclaration(CONFIG_NAME, null, null, listValue);
    assertSampleDataSuccess(elementDeclaration, null,
                            format("%s-%s-%s", "defaultStringValue", intProviderDefaultValue, listValue.get(0)));
  }

  @Test
  public void actingParameterGroupWithOptionalProviderParamOperationExplicitValueInDSL() {
    String stringValue = "explicitStringValue";
    List<String> listValue = singletonList("single");
    Integer intExplicitValue = 99;
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupWithOptionalWithDefaultInContainerProviderParamOPDeclaration(CONFIG_NAME, stringValue,
                                                                                         intExplicitValue, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s-%s", stringValue, intExplicitValue, listValue.get(0)));
  }

  @Test
  public void actingParameterGroupWithAliasOperation() {
    String stringValue = "stringValue";
    int intValue = 1;
    List<String> listValue = singletonList("single");
    ComponentElementDeclaration<?> elementDeclaration =
        actingParameterGroupOPWithAliasDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    assertSampleDataSuccess(elementDeclaration, null, format("%s-%s--100-%s", stringValue, intValue, listValue.get(0)));
  }

  @Test
  public void customErrorCodeFromProvider() {
    ComponentElementDeclaration<?> elementDeclaration = errorSampleDataOP(CONFIG_NAME);
    String message = "Expected error";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    assertSampleDataFailure(elementDeclaration, message, reason, CUSTOM_ERROR_CODE);
  }

  @Test
  public void actingParameterConfigConnectionSource() {
    ComponentElementDeclaration<?> elementDeclaration = sourceDeclaration(CONFIG_NAME, "actingParameter");
    assertSampleDataSuccessWithMatcher(elementDeclaration, "client-actingParameter", new BaseMatcher() {

      @Override
      public boolean matches(Object o) {
        Supplier<String> supplier = (Supplier<String>) o;
        return "dummyConfig".equals(supplier.get());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Acting parameter didn't match");
      }
    });
  }

  @Test
  public void connectionFailure() {
    assertSampleDataFailure(configLessOPDeclaration(CONFIG_FAILING_CONNECTION_PROVIDER),
                            "Failed to establish connection: org.mule.runtime.api.connection.ConnectionException: Expected connection exception",
                            "org.mule.sdk.api.data.sample.SampleDataException: Failed to establish connection: org.mule.runtime.api.connection.ConnectionException: Expected connection exception",
                            "CONNECTION_FAILURE");
  }

  @Test
  public void sampleDataWithBindingOnTopLevel() {
    final String ap = "sampleData!";
    assertSampleDataSuccess(vpWithBindingToTopLevelOPDeclaration(ap), ap, null);
  }

  @Test
  public void sampleDataWithBindingOnPojoField() {
    final String ap = "sampleData!";
    assertSampleDataSuccess(vpOnPojoDependsOnOwnFieldOPDeclaration(ap), ap, null);
  }

  @Test
  public void sampleDataWithBindingOnContentField() {
    final String ap = "SampleData!";
    assertSampleDataSuccess(vpOnContentDependsOnOwnFieldOPDeclaration("#[{field:'" + ap + "'}]"), ap, null);
  }

  @Test
  public void sampleDataWithBindingOnContentDeepField() {
    final String ap = "SampleData!";
    assertSampleDataSuccess(vpOnContentDependsOnOwnDeepFieldOPDeclaration("#[{'levelOne': {'levelTwo': {'field':'" + ap
        + "'}}}]"), ap, null);
  }

  @Test
  public void sampleDataWithBindingOnContentDeepFieldAsPojo() {
    final String ap = "SampleData!";
    assertSampleDataSuccess(vpOnContentDependsOnOwnDeepFieldAsPojoOPDeclaration(
                                                                                "#[{" +
                                                                                    "    'levelOne': { " +
                                                                                    "        'complexField':{" +
                                                                                    "            'stringParam':'" + ap + "'," +
                                                                                    "            'innerPojoParam': { " +
                                                                                    "                'stringParam':'" + ap + "'" +
                                                                                    "            }" +
                                                                                    "        }" +
                                                                                    "    }" +
                                                                                    "}]"),
                            null, "0" + ap + "0" + ap);
  }

  @Test
  public void sampleDataWithBindingOnContentAttribute() {
    final String ap = "SampleData!";
    assertSampleDataSuccess(vpOnContentDependsOnOwnAttributeOPDeclaration("#[{field @(attribute: '" + ap + "'): 'dont-care'}]"),
                            ap, null);
  }

  @Test
  public void sampleDataWithBindingOnStreamField() {
    final String ap = "SampleData!";
    assertSampleDataSuccess(vpOnStreamDependsOnOwnFieldOPDeclaration("#[{'field':'" + ap + "'}]"), ap, null);
  }

  private void assertSampleDataSuccess(ComponentElementDeclaration<?> elementDeclaration,
                                       String expectedPayload,
                                       Object expectedAttributes) {
    assertSampleDataSuccessWithMatcher(elementDeclaration, expectedPayload, is(expectedAttributes));
  }

  private void assertSampleDataSuccessWithMatcher(ComponentElementDeclaration<?> elementDeclaration,
                                                  String expectedPayload,
                                                  Matcher matcher) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(true));
    assertThat(sampleData.getSampleData().isPresent(), is(true));
    Message message = sampleData.getSampleData().get();

    assertThat(message.getPayload().getValue(), is(expectedPayload));
    assertThat(message.getAttributes().getValue(), matcher);
  }

  private void assertSampleDataFailure(ComponentElementDeclaration<?> elementDeclaration, String expectedMessage,
                                       String expectedReason, String expectedCode) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(false));
    assertThat(sampleData.getFailure().isPresent(), is(true));

    SampleDataFailure sampleDataFailure = sampleData.getFailure().get();
    assertThat(sampleDataFailure.getMessage(), is(expectedMessage));
    assertThat(sampleDataFailure.getReason(), containsString(expectedReason));
    assertThat(sampleDataFailure.getFailureCode(), is(expectedCode));
  }
}
