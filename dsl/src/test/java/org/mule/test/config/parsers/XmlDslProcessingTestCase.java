/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.parsers;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.tests.api.pojos.ElementWithAttributeAndChild;
import org.mule.tests.api.pojos.MyPojo;
import org.mule.tests.api.pojos.ParameterCollectionParser;
import org.mule.tests.api.pojos.SameChildTypeContainer;
import org.mule.tests.api.pojos.TextPojo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.mule.tests:mule-derby-all",
    "org.mule.tests:mule-activemq-broker"
})
public class XmlDslProcessingTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/parsers/xml-dsl-processing-config.xml";
  }

  @Test
  public void onlySimpleParametersInSingleAttribute() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("onlySimpleParametersObject").get();
    assertPabloChildParameters(parsersTestObject);
  }

  @Test
  public void firstComplexChildUsingWrapper() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("onlyComplexFirstChildParameterObject").get();
    assertPabloChildParameters(parsersTestObject.getFirstChild());
  }

  @Test
  public void secondComplexChildUsingWrapper() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("onlyComplexSecondChildParameterObject").get();
    assertMarianoChildParameters(parsersTestObject.getSecondChild());
  }

  @Test
  public void complexChildrenListUsingWrapper() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("onlyComplexChildrenListParameterObject").get();
    assertCollectionChildrenContent(parsersTestObject.getOtherChildren());
  }

  @Test
  public void completeParametersObject() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("completeParametersObject").get();
    assertPabloChildParameters(parsersTestObject);
    assertPabloChildParameters(parsersTestObject.getFirstChild());
    assertMarianoChildParameters(parsersTestObject.getSecondChild());
    assertCollectionChildrenContent(parsersTestObject.getOtherChildren());
  }

  @Test
  public void customCollectionTypeObject() {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("customCollectionTypeObject").get();
    List<ParameterCollectionParser> collectionObject = parsersTestObject.getOtherChildrenCustomCollectionType();
    assertThat(collectionObject, instanceOf(LinkedList.class));
    assertCollectionChildrenContent(collectionObject);
  }

  @Test
  public void simpleTypeObject() {
    ParameterCollectionParser parsersTestObject = registry.<ParameterCollectionParser>lookupByName("simpleTypeObject").get();
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeChildList());
    assertThat(parsersTestObject.getSimpleTypeChildSet(), instanceOf(Set.class));
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeChildSet());
    assertSimpleTypeCollectionValues(parsersTestObject.getOtherSimpleTypeChildList());
  }

  @Test
  public void simpleTypeMapObject() {
    ParameterCollectionParser parsersTestObject = registry.<ParameterCollectionParser>lookupByName("simpleTypeMapObject").get();
    Map<String, Integer> simpleTypeMap = parsersTestObject.getSimpleTypeEntry();
    assertThat(simpleTypeMap.size(), is(2));
  }

  @Test
  public void simpleListTypeMapObject() throws MuleException {
    ParameterCollectionParser parsersTestObject =
        registry.<ParameterCollectionParser>lookupByName("simpleTypeCollectionMapObject").get();
    Map<String, List<String>> simpleListTypeMap = parsersTestObject.getSimpleListTypeEntry();
    assertThat(simpleListTypeMap.size(), is(2));
    List<String> firstCollection = simpleListTypeMap.get("1");
    assertThat(firstCollection, hasItems("value1", "value2"));
    List<String> secondCollection = simpleListTypeMap.get("2");
    assertThat(secondCollection, hasItems("some value"));
  }

  @Test
  @Ignore("MULE-18586")
  public void complexTypeMapObject() {
    ParameterCollectionParser parsersTestObject = registry.<ParameterCollectionParser>lookupByName("complexTypeMapObject").get();
    Map<Long, ParameterCollectionParser> simpleTypeMap = parsersTestObject.getComplexTypeEntry();
    assertThat(simpleTypeMap.size(), is(2));
    assertPabloChildParameters(simpleTypeMap.get(1l));
    assertMarianoChildParameters(simpleTypeMap.get(2l));
  }

  @Test
  public void pojoWithDefaultValue() {
    ElementWithAttributeAndChild parameterAndChildElement =
        registry.<ElementWithAttributeAndChild>lookupByName("pojoWithDefaultValue").get();
    assertThat(parameterAndChildElement.getMyPojo(), is(new MyPojo("jose")));
  }

  @Test
  public void pojoFromConfiguraitonParameter() {
    ElementWithAttributeAndChild parameterAndChildElement =
        registry.<ElementWithAttributeAndChild>lookupByName("pojoWithAttribute").get();
    assertThat(parameterAndChildElement.getMyPojo(), is(new MyPojo("pepe")));
  }

  @Test
  public void pojoFromChildConfiguration() {
    ElementWithAttributeAndChild parameterAndChildElement =
        registry.<ElementWithAttributeAndChild>lookupByName("pojoWithChild").get();
    assertThat(parameterAndChildElement.getMyPojo(), is(new MyPojo("pepe")));
  }

  @Test
  public void objectWithTwoChildrenOfSameTypeWithoutWrapper() {
    SameChildTypeContainer pojoWithSameTypeChildren =
        registry.<SameChildTypeContainer>lookupByName("sameChildTypesObject").get();
    assertPabloChildParameters(pojoWithSameTypeChildren.getElementTypeA());
    assertMarianoChildParameters(pojoWithSameTypeChildren.getAnotherElementTypeA());
  }

  @Test
  public void textPojo() {
    TextPojo pojo = registry.<TextPojo>lookupByName("textPojo").get();
    assertThat(pojo, is(notNullValue()));
    assertThat(pojo.getSomeParameter(), is("select * from PLANET"));
  }

  private void assertSimpleTypeCollectionValues(Collection<String> simpleTypeCollectionValues) {
    assertThat(simpleTypeCollectionValues.size(), is(2));
    assertThat(simpleTypeCollectionValues, hasItems("value1", "value2"));
  }

  private void assertCollectionChildrenContent(List<ParameterCollectionParser> collectionObjects) {
    assertPabloChildParameters(collectionObjects.get(0));
    assertMarianoChildParameters(collectionObjects.get(1));
  }

  private void assertPabloChildParameters(ParameterCollectionParser simpleParameters) {
    assertThat(simpleParameters.getFirstname(), is("Pablo"));
    assertThat(simpleParameters.getLastname(), is("La Greca"));
    assertThat(simpleParameters.getAge(), is(32));
  }

  private void assertMarianoChildParameters(ParameterCollectionParser simpleParameters) {
    assertThat(simpleParameters.getFirstname(), is("Mariano"));
    assertThat(simpleParameters.getLastname(), is("Gonzalez"));
    assertThat(simpleParameters.getAge(), is(31));
  }
}
