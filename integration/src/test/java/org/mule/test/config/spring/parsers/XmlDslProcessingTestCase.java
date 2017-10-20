/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.tests.parsers.api.ParameterAndChildElement;
import org.mule.tests.parsers.api.ParsersTestObject;
import org.mule.tests.parsers.api.PojoWithSameTypeChildren;
import org.mule.tests.parsers.api.SimplePojo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Test;

public class XmlDslProcessingTestCase extends AbstractIntegrationTestCase implements IntegrationTestCaseRunnerConfig {

  private static final String FIRST_NAME_ATTRIBUTE = "firstname";
  private static final String LAST_NAME_ATTRIBUTE = "lastname";
  private static final String AGE_ATTRIBUTE = "age";

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/xml-dsl-processing-config.xml";
  }

  @Test
  public void onlySimpleParametersInSingleAttribute() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("onlySimpleParametersObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(3));
    assertPabloChildParameters(simpleParameters);
  }

  @Test
  public void firstComplexChildUsingWrapper() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("onlyComplexFirstChildParameterObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertPabloChildParameters(((ParsersTestObject) simpleParameters.get("first-child")).getSimpleParameters());
  }

  @Test
  public void secondComplexChildUsingWrapper() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("onlyComplexSecondChildParameterObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertMarianoChildParameters(((ParsersTestObject) simpleParameters.get("second-child")).getSimpleParameters());
  }

  @Test
  public void complexChildrenListUsingWrapper() {
    ParsersTestObject parsersTestObject =
        registry.<ParsersTestObject>lookupByName("onlyComplexChildrenListParameterObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertCollectionChildrenContent((List<ParsersTestObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void completeParametersObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("completeParametersObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(6));
    assertPabloChildParameters(simpleParameters);
    assertPabloChildParameters(((ParsersTestObject) simpleParameters.get("first-child")).getSimpleParameters());
    assertMarianoChildParameters(((ParsersTestObject) simpleParameters.get("second-child")).getSimpleParameters());
    assertCollectionChildrenContent((List<ParsersTestObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void customCollectionTypeObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("customCollectionTypeObject").get();
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    List<ParsersTestObject> collectionObject =
        (List<ParsersTestObject>) simpleParameters.get("other-children-custom-collection-type");
    assertThat(collectionObject, instanceOf(LinkedList.class));
    assertCollectionChildrenContent(collectionObject);
  }

  @Test
  public void simpleTypeObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("simpleTypeObject").get();
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeList());
    assertThat(parsersTestObject.getSimpleTypeSet(), instanceOf(TreeSet.class));
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeSet());
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertSimpleTypeCollectionValues((List<String>) simpleParameters.get("other-simple-type-child-list-custom-key"));
  }

  @Test
  public void simpleTypeChildListWithConverter() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("simpleTypeObjectWithConverter").get();
    List<String> simpleTypeListWithConverter = parsersTestObject.getSimpleTypeListWithConverter();
    assertThat(simpleTypeListWithConverter.size(), is(2));
    assertThat(simpleTypeListWithConverter, hasItems("value1-with-converter", "value2-with-converter"));
  }

  @Test
  public void simpleTypeMapObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("simpleTypeMapObject").get();
    Map<String, Integer> simpleTypeMap = parsersTestObject.getSimpleTypeMap();
    assertThat(simpleTypeMap.size(), is(2));
  }

  @Test
  public void simpleListTypeMapObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("simpleTypeCollectionMapObject").get();
    Map<String, List<String>> simpleListTypeMap = parsersTestObject.getSimpleListTypeMap();
    assertThat(simpleListTypeMap.size(), is(2));
    List<String> firstCollection = simpleListTypeMap.get("1");
    assertThat(firstCollection, hasItems("value1", "value2"));
    List<String> secondCollection = simpleListTypeMap.get("2");
    assertThat(secondCollection, hasItem("#[mel:'some expression']"));
  }

  @Test
  public void complexTypeMapObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("complexTypeMapObject").get();
    Map<Long, ParsersTestObject> simpleTypeMap = parsersTestObject.getComplexTypeMap();
    assertThat(simpleTypeMap.size(), is(2));
    assertPabloChildParameters(simpleTypeMap.get(1l).getSimpleParameters());
    assertMarianoChildParameters(simpleTypeMap.get(2l).getSimpleParameters());
  }

  @Test
  public void pojoWithDefaultValue() {
    ParameterAndChildElement parameterAndChildElement =
        registry.<ParameterAndChildElement>lookupByName("pojoWithDefaultValue").get();
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("jose")), is(true));
  }

  @Test
  public void pojoFromConfiguraitonParameter() {
    ParameterAndChildElement parameterAndChildElement =
        registry.<ParameterAndChildElement>lookupByName("pojoWithAttribute").get();
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("pepe")), is(true));
  }

  @Test
  public void pojoFromChildConfiguration() {
    ParameterAndChildElement parameterAndChildElement = registry.<ParameterAndChildElement>lookupByName("pojoWithChild").get();
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("pepe")), is(true));
  }

  @Test
  public void objectWithTwoChildrenOfSameTypeWithoutWrapper() {
    PojoWithSameTypeChildren pojoWithSameTypeChildren =
        registry.<PojoWithSameTypeChildren>lookupByName("sameChildTypesObject").get();
    assertPabloChildParameters(pojoWithSameTypeChildren.getElementTypeA().getSimpleParameters());
    assertMarianoChildParameters(pojoWithSameTypeChildren.getAnotherElementTypeA().getSimpleParameters());
  }

  @Test
  public void textPojo() {
    SimplePojo pojo = registry.<SimplePojo>lookupByName("textPojo").get();
    assertThat(pojo, is(notNullValue()));
    assertThat(pojo.getSomeParameter(), is("select * from PLANET"));
  }

  @Test
  public void simpleTypeWithConverterObject() {
    ParsersTestObject parsersTestObject = registry.<ParsersTestObject>lookupByName("simpleTypeWithConverterObject").get();
    assertThat(parsersTestObject.getSimpleTypeWithConverter(), is(new SimplePojo("5")));
  }

  private void assertSimpleTypeCollectionValues(Collection<String> simpleTypeCollectionValues) {
    assertThat(simpleTypeCollectionValues.size(), is(2));
    assertThat(simpleTypeCollectionValues, hasItems("value1", "value2"));
  }

  private void assertCollectionChildrenContent(List<ParsersTestObject> collectionObjects) {
    assertPabloChildParameters(collectionObjects.get(0).getSimpleParameters());
    assertMarianoChildParameters(collectionObjects.get(1).getSimpleParameters());
  }

  private void assertPabloChildParameters(Map<Object, Object> simpleParameters) {
    assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Pablo"));
    assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("La Greca"));
    assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("32"));
  }

  private void assertMarianoChildParameters(Map<Object, Object> simpleParameters) {
    assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Mariano"));
    assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("Gonzalez"));
    assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("31"));
  }
}
