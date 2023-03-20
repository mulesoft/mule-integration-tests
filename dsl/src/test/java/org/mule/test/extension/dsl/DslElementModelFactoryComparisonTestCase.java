/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

public class DslElementModelFactoryComparisonTestCase extends AbstractElementModelTestCase {

  private static final String OPERATION_NAME = "withComplexActingParameter";
  private static final String COMPLEX_ACTING_PARAMETER_NAME = "complexActingParameter";

  private static final String INT_PARAM_NAME = "intParam";
  private static final String STRING_PRAM_NAME = "stringParam";
  private static final String INNER_POJO_PARAM_NAME = "innerPojoParam";
  private static final String MAP_PARAM_NAME = "mapParam";
  private static final String LIST_PARAM_NAME = "listParam";
  private static final String COMPLEX_MAP_PARAM_NAME = "complexMapParam";
  private static final String COMPLEX_LIST_PARAM_NAME = "complexListParam";

  @Before
  public void initApp() throws Exception {
    applicationModel = loadApplicationModel();
  }

  @Override
  protected String getConfigFile() {
    return "comparison-config.xml";
  }

  private ParameterValue declareInnerPojo() {
    return ParameterObjectValue.builder()
        .withParameter(INT_PARAM_NAME, "0")
        .withParameter(STRING_PRAM_NAME, "zero")
        .withParameter(LIST_PARAM_NAME, ParameterListValue
            .builder()
            .withValue("zero")
            .withValue("one")
            .withValue("two")
            .build())
        .withParameter(MAP_PARAM_NAME,
                       ParameterObjectValue
                           .builder()
                           .withParameter("0", "zero")
                           .withParameter("1", "one")
                           .withParameter("2", "two")
                           .build())
        .build();
  }

  private void validateInnerPojo(DslElementModel<?> innerPojoModel) {
    assertThat(innerPojoModel.getContainedElements(), hasSize(4));

    DslElementModel<?> intParam = getElement(INT_PARAM_NAME, innerPojoModel.getContainedElements());
    assertThat(intParam.getValue().get(), is("0"));

    DslElementModel<?> stringParam = getElement(STRING_PRAM_NAME, innerPojoModel.getContainedElements());
    assertThat(stringParam.getValue().get(), is("zero"));

    DslElementModel<?> listParam = getElement(LIST_PARAM_NAME, innerPojoModel.getContainedElements());
    assertThat(listParam.getContainedElements(), hasSize(3));
    validateListEntry(listParam.getContainedElements().get(0), "zero");
    validateListEntry(listParam.getContainedElements().get(1), "one");
    validateListEntry(listParam.getContainedElements().get(2), "two");

    DslElementModel<?> mapParam = getElement(MAP_PARAM_NAME, innerPojoModel.getContainedElements());
    assertThat(mapParam.getContainedElements(), hasSize(3));
    validateMapEntry(mapParam.getContainedElements().get(0), "0", v -> assertThat(v.getValue().get(), is("zero")));
    validateMapEntry(mapParam.getContainedElements().get(1), "1", v -> assertThat(v.getValue().get(), is("one")));
    validateMapEntry(mapParam.getContainedElements().get(2), "2", v -> assertThat(v.getValue().get(), is("two")));
  }

  private DslElementModel getElement(String attributeName, List<DslElementModel> allElements) {
    return allElements.stream().filter(e -> e.getDsl().getAttributeName().equals(attributeName)).findAny().get();
  }

  private void validateListEntry(DslElementModel<?> listEntry, String expectedValue) {
    assertThat(listEntry.getContainedElements(), hasSize(1));
    DslElementModel<?> valueElement = listEntry.getContainedElements().get(0);
    assertThat(valueElement.getDsl().getAttributeName(), is("value"));
    assertThat(valueElement.getValue().get(), is(expectedValue));
  }

  private void validateMapEntry(DslElementModel<?> mapEntry, String expectedKey, Consumer<DslElementModel> valueValidator) {
    assertThat(mapEntry.getContainedElements(), hasSize(2));
    DslElementModel keyElement = mapEntry.getContainedElements().get(0);
    DslElementModel valueElement = mapEntry.getContainedElements().get(1);

    assertThat(keyElement.getDsl().getAttributeName(), is("key"));
    assertThat(valueElement.getDsl().getAttributeName(), is("value"));

    assertThat(keyElement.getValue().get(), is(expectedKey));
    valueValidator.accept(valueElement);
  }

  private void validateDsl(DslElementModel<OperationModel> dslElementModel) {
    assertThat(dslElementModel.getModel().getName(), is(OPERATION_NAME));
    assertThat(dslElementModel.getContainedElements().toString(),
               dslElementModel.getContainedElements(), hasSize(1));

    DslElementModel<ParameterModel> complexActingParameterModel = dslElementModel.getContainedElements().get(0);
    assertThat(complexActingParameterModel.getModel().getName(), is(COMPLEX_ACTING_PARAMETER_NAME));

    assertThat(complexActingParameterModel.getContainedElements().toString(),
               complexActingParameterModel.getContainedElements(), hasSize(7));

    DslElementModel<?> innerPojo = getElement(INNER_POJO_PARAM_NAME, complexActingParameterModel.getContainedElements());
    validateInnerPojo(innerPojo);

    DslElementModel<?> intParam = getElement(INT_PARAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(intParam.getValue().get(), is("0"));

    DslElementModel<?> stringParam = getElement(STRING_PRAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(stringParam.getValue().get(), is("zero"));

    DslElementModel<?> complexMap = getElement(COMPLEX_MAP_PARAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(complexMap.getContainedElements(), hasSize(2));
    validateMapEntry(complexMap.getContainedElements().get(0), "0", this::validateInnerPojo);
    validateMapEntry(complexMap.getContainedElements().get(1), "1", this::validateInnerPojo);

    DslElementModel<?> complexList = getElement(COMPLEX_LIST_PARAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(complexList.getContainedElements(), hasSize(1));
    complexList.getContainedElements().forEach(this::validateInnerPojo);

    DslElementModel<?> listParam = getElement(LIST_PARAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(listParam.getContainedElements(), hasSize(3));
    validateListEntry(listParam.getContainedElements().get(0), "zero");
    validateListEntry(listParam.getContainedElements().get(1), "one");
    validateListEntry(listParam.getContainedElements().get(2), "two");

    DslElementModel<?> mapParam = getElement(MAP_PARAM_NAME, complexActingParameterModel.getContainedElements());
    assertThat(mapParam.getContainedElements(), hasSize(3));
    validateMapEntry(mapParam.getContainedElements().get(0), "0", v -> assertThat(v.getValue().get(), is("zero")));
    validateMapEntry(mapParam.getContainedElements().get(1), "1", v -> assertThat(v.getValue().get(), is("one")));
    validateMapEntry(mapParam.getContainedElements().get(2), "2", v -> assertThat(v.getValue().get(), is("two")));
  }

  @Test
  public void compareModels() {
    ElementDeclarer values = ElementDeclarer.forExtension("Values");
    ParameterValue complexParameterValue = ParameterObjectValue.builder()
        .withParameter(COMPLEX_LIST_PARAM_NAME, ParameterListValue
            .builder()
            .withValue(declareInnerPojo())
            .build())
        .withParameter(COMPLEX_MAP_PARAM_NAME, ParameterObjectValue
            .builder()
            .withParameter("0", declareInnerPojo())
            .withParameter("1", declareInnerPojo())
            .build())
        .withParameter(INNER_POJO_PARAM_NAME, declareInnerPojo())
        .withParameter(INT_PARAM_NAME, "0")
        .withParameter(STRING_PRAM_NAME, "zero")
        .withParameter(LIST_PARAM_NAME, ParameterListValue
            .builder()
            .withValue("zero")
            .withValue("one")
            .withValue("two")
            .build())
        .withParameter(MAP_PARAM_NAME, ParameterObjectValue
            .builder()
            .withParameter("0", "zero")
            .withParameter("1", "one")
            .withParameter("2", "two")
            .build())
        .build();

    OperationElementDeclaration complexActingParameterOperation = values
        .newOperation(OPERATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(COMPLEX_ACTING_PARAMETER_NAME, complexParameterValue)
            .getDeclaration())
        .getDeclaration();
    ComponentAst flow = getAppElement(applicationModel, COMPONENTS_FLOW);
    ComponentAst operationAst = flow.directChildrenStream().findFirst().get();

    DslElementModel<OperationModel> astDsl = resolve(operationAst);
    DslElementModel<OperationModel> declarationDsl = resolve(complexActingParameterOperation);

    validateDsl(declarationDsl);
    validateDsl(astDsl);
  }

  @Test
  public void repeatedElementsAreNotPopulated() {
    ElementDeclarer values = ElementDeclarer.forExtension("Values");
    ParameterValue complexParameterValue = ParameterObjectValue.builder()
        .withParameter(LIST_PARAM_NAME, ParameterListValue
            .builder()
            .withValue("one")
            .withValue("one")
            .withValue("one")
            .build())
        .build();

    OperationElementDeclaration complexActingParameterOperation = values
        .newOperation(OPERATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(COMPLEX_ACTING_PARAMETER_NAME, complexParameterValue)
            .getDeclaration())
        .getDeclaration();
    DslElementModel<OperationModel> declarationDsl = resolve(complexActingParameterOperation);
    assertThat(declarationDsl.getContainedElements(), hasSize(1));
    DslElementModel<?> complexActingParameter = declarationDsl.getContainedElements().get(0);
    assertThat(complexActingParameter.getDsl().getAttributeName(), is(COMPLEX_ACTING_PARAMETER_NAME));
    assertThat(complexActingParameter.getContainedElements(), hasSize(1));
    DslElementModel<?> listParam = complexActingParameter.getContainedElements().get(0);
    assertThat(listParam.getDsl().getAttributeName(), is(LIST_PARAM_NAME));
    assertThat(listParam.getContainedElements().toString(), listParam.getContainedElements(), hasSize(3));
  }

}
