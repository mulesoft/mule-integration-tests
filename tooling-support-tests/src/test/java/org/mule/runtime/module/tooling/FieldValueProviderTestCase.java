/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling;

import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDeepFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnAttributeOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnDeepFieldAsPojoOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnDeepFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentDependsOnTopLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnContentMultipleFieldsOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoDeepFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoDependsOnOtherFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoDependsOnTopLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoMultilevelFieldsOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnPojoMultipleFieldsOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamDeepFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamDependsOnOwnFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamDependsOnTopLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamFieldOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.vpOnStreamMultipleFieldsOPDeclaration;
import static org.mule.sdk.api.values.ValueResolvingException.INVALID_VALUE_RESOLVER_NAME;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;

import org.junit.Test;

public class FieldValueProviderTestCase extends DeclarationSessionTestCase {

  @Test
  public void vpOnPojoWrongTargetSelector() {
    OperationElementDeclaration operationDeclaration = vpOnPojoFieldOPDeclaration();
    validateFieldValueFailure(operationDeclaration, "innerPojo", "some.wrong.selector",
                              "The parameter with name 'innerPojo' does not have a Value Provider associated with the targetSelector 'some.wrong.selector'",
                              INVALID_VALUE_RESOLVER_NAME);
  }

  @Test
  public void vpOnPojoWrongParameterName() {
    OperationElementDeclaration operationDeclaration = vpOnPojoFieldOPDeclaration();
    ValueResult providerResult = getFieldValues(session, operationDeclaration, "wrongParameter", "dont.care");
    assertThat(providerResult.isSuccess(), equalTo(false));
    assertThat(providerResult.getFailure().get().getMessage(),
               equalTo("Unable to find model for parameter or parameter group with name 'wrongParameter'."));
    assertThat(providerResult.getFailure().get().getFailureCode(), equalTo(INVALID_VALUE_RESOLVER_NAME));
  }

  @Test
  public void vpOnPojoField() {
    OperationElementDeclaration operationDeclaration = vpOnPojoFieldOPDeclaration();
    validateFieldValuesSuccess(operationDeclaration, "innerPojo", "stringParam", "simpleValue");
  }

  @Test
  public void vpOnPojoDeepField() {
    OperationElementDeclaration operationDeclaration = vpOnPojoDeepFieldOPDeclaration();
    validateFieldValuesSuccess(operationDeclaration, "complexActingParameter", "innerPojoParam.stringParam", "simpleValue");
  }

  @Test
  public void vpOnPojoDependsOnTopLevel() {
    final String actingParameter = "actingParameter";
    OperationElementDeclaration operationDeclaration = vpOnPojoDependsOnTopLevelOPDeclaration(actingParameter);
    validateFieldValuesSuccess(operationDeclaration, "innerPojo", "stringParam", actingParameter);
  }

  @Test
  public void vpOnPojoDependsOnOwnField() {
    final String actingParameter = "actingParameter";
    OperationElementDeclaration operationDeclaration = vpOnPojoDependsOnOwnFieldOPDeclaration(actingParameter);
    validateFieldValuesSuccess(operationDeclaration, "complexActingParameter", "stringParam", actingParameter);
  }

  @Test
  public void vpOnPojoDependsOnOtherField() {
    final String actingParameter = "actingParameter";
    OperationElementDeclaration operationDeclaration = vpOnPojoDependsOnOtherFieldOPDeclaration(actingParameter);
    validateFieldValuesSuccess(operationDeclaration, "innerPojo", "stringParam", actingParameter);
  }

  @Test
  public void vpOnPojoMultipleFields() {
    OperationElementDeclaration operationDeclaration = vpOnPojoMultipleFieldsOPDeclaration();
    validateFieldValuesSuccess(operationDeclaration, "complexActingParameter", "stringParam", "simpleValue");
    validateFieldValuesSuccess(operationDeclaration, "complexActingParameter", "innerPojoParam.stringParam", "simpleValue");
  }

  @Test
  public void vpOnPojoMultiLevelFields() {
    // ValueResult for multi level no matter if previous levels are set it would always return the whole tree
    validateFieldValuesSuccess(vpOnPojoMultilevelFieldsOPDeclaration(null, null, null), "multiLevelValue", "continent",
                               "America");
    validateFieldValuesSuccess(vpOnPojoMultilevelFieldsOPDeclaration("America", null, null), "multiLevelValue", "country",
                               "America");
    validateFieldValuesSuccess(vpOnPojoMultilevelFieldsOPDeclaration("America", "USA", null), "multiLevelValue", "city",
                               "America");
  }

  @Test
  public void vpOnContentField() {
    OperationElementDeclaration operationElementDeclaration = vpOnContentFieldOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "simpleValue");
  }

  @Test
  public void vpOnContentDeepField() {
    OperationElementDeclaration operationElementDeclaration = vpOnContentDeepFieldOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "innerPojoParam.stringParam", "simpleValue");
  }

  @Test
  public void vpOnContentDependsOnTopLevel() {
    final String actingParameter = "actingParameter";
    OperationElementDeclaration operationElementDeclaration = vpOnContentDependsOnTopLevelOPDeclaration(actingParameter);
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", actingParameter);
  }

  @Test
  public void vpOnContentDependsOnOwnField() {
    OperationElementDeclaration operationElementDeclaration =
        vpOnContentDependsOnOwnFieldOPDeclaration("#[{'field':'value'}]");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "value");
  }

  @Test
  public void vpOnContentDependsOnOwnAttribute() {
    OperationElementDeclaration operationElementDeclaration =
        vpOnContentDependsOnOwnAttributeOPDeclaration("#[{field @(attribute: 'value'): 'dont-care'}]");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "value");
  }

  @Test
  public void vpOnContentDependsOnOwnDeepField() {
    OperationElementDeclaration operationElementDeclaration =
        vpOnContentDependsOnOwnDeepFieldOPDeclaration("#[{'levelOne': {'levelTwo': {'field':'value'}}}]");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "value");
  }

  @Test
  public void vpOnContentDependsOnOwnDeepFieldAsPojo() {
    OperationElementDeclaration operationElementDeclaration =
        vpOnContentDependsOnOwnDeepFieldAsPojoOPDeclaration(
                                                            "#[{" +
                                                                "    'levelOne': { " +
                                                                "        'complexField':{" +
                                                                "            'stringParam':'value'," +
                                                                "            'innerPojoParam': { " +
                                                                "                'stringParam':'value'" +
                                                                "            }" +
                                                                "        }" +
                                                                "    }" +
                                                                "}]");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "0value0value");
  }

  @Test
  public void vpOnContentMultipleFields() {
    OperationElementDeclaration operationElementDeclaration = vpOnContentMultipleFieldsOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "innerPojoParam.stringParam", "simpleValue");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "simpleValue");
  }

  @Test
  public void vpOnStreamField() {
    OperationElementDeclaration operationElementDeclaration = vpOnStreamFieldOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "simpleValue");
  }

  @Test
  public void vpOnStreamDeepField() {
    OperationElementDeclaration operationElementDeclaration = vpOnStreamDeepFieldOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "innerPojoParam.stringParam", "simpleValue");
  }

  @Test
  public void vpOnStreamDependsOnTopLevel() {
    final String actingParameter = "actingParameter";
    OperationElementDeclaration operationElementDeclaration = vpOnStreamDependsOnTopLevelOPDeclaration(actingParameter);
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", actingParameter);
  }

  @Test
  public void vpOnStreamDependsOwnFieldLevel() {
    OperationElementDeclaration operationElementDeclaration = vpOnStreamDependsOnOwnFieldOPDeclaration("#[{'field':'value'}]");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "value");
  }

  @Test
  public void vpOnStreamMultipleFields() {
    OperationElementDeclaration operationElementDeclaration = vpOnStreamMultipleFieldsOPDeclaration();
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "innerPojoParam.stringParam", "simpleValue");
    validateFieldValuesSuccess(operationElementDeclaration, "parameter", "stringParam", "simpleValue");
  }


  private void validateFieldValuesSuccess(ComponentElementDeclaration<?> elementDeclaration,
                                          String parameterName,
                                          String targetSelector,
                                          String... expectedValues) {
    ValueResult providerResult = getFieldValues(session, elementDeclaration, parameterName, targetSelector);
    assertThat(providerResult.isSuccess(), equalTo(true));
    assertThat(providerResult.getValues(), hasSize(expectedValues.length));
    assertThat(providerResult.getValues().stream().map(Value::getId).collect(toList()),
               containsInAnyOrder(expectedValues));
  }

  private void validateFieldValueFailure(ComponentElementDeclaration<?> elementDeclaration,
                                         String parameterName,
                                         String targetSelector,
                                         String expectedMessage,
                                         String expectedCode) {
    ValueResult providerResult = getFieldValues(session, elementDeclaration, parameterName, targetSelector);
    assertThat(providerResult.isSuccess(), equalTo(false));
    assertThat(providerResult.getFailure().get().getMessage(), equalTo(expectedMessage));
    assertThat(providerResult.getFailure().get().getFailureCode(), equalTo(expectedCode));
  }


  private ValueResult getFieldValues(DeclarationSession declarationSession, ComponentElementDeclaration<?> elementDeclaration,
                                     String parameterName, String targetSelector) {
    return declarationSession.getFieldValues(elementDeclaration, parameterName, targetSelector);
  }

}
