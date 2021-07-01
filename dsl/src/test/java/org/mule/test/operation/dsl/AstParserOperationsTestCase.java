/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation.dsl;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OUTPUT_PAYLOAD_TYPE;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;

import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.CoreRuntimeExtensionModelProvider;
import org.mule.runtime.core.api.extension.OperationDslExtensionModelProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AstParserOperationsTestCase {

  private ClassLoader classLoader;
  private AstXmlParser parser;

  private final Map<String, String> properties = new HashMap<>();
  private ArtifactAst ast;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    properties.clear();
    classLoader = getClass().getClassLoader();

    parser = builder()
        .withSchemaValidationsDisabled()
        .withPropertyResolver(propertyKey -> properties.getOrDefault(propertyKey, propertyKey))
        .withExtensionModel(new CoreRuntimeExtensionModelProvider().createExtensionModel())
        .withExtensionModel(new OperationDslExtensionModelProvider().createExtensionModel())
        .build();

    ast = parser.parse(classLoader.getResource("org/mule/test/config/operation/mule-operations.xml"));
  }

  @Test
  public void parseHelloOperation() {
    ComponentAst operation = findOperationByName(ast, "hello");
    assertHelloOperationStructure(operation);
  }

  @Test
  public void parseOperationWithAttributesDefinition() {
    ComponentAst operation = findOperationByName(ast, "helloWithAttributes");
    assertHelloOperationStructure(operation);

    ComponentAst attributesType = singleNode(operation.directChildrenStreamByIdentifier(null, "output")
            .flatMap(c -> c.directChildrenStreamByIdentifier(null, "attributes-type")));

    assertSimpleParameter(attributesType, "type", "OBJECT");
    assertSimpleParameter(attributesType, "mimeType", "application/json");
  }

  private void assertHelloOperationStructure(ComponentAst operation) {
    assertThat(operation.getComponentType(), equalTo(OPERATION_DEF));
    assertThat(operation.getParameter("public").getValue().getRight(), is(false));
    assertThat(operation.getModel(ConstructModel.class).isPresent(), is(true));
    ComponentAst parameter  = singleNode(operation.directChildrenStreamByIdentifier(null, "parameters")
            .flatMap(c -> c.directChildrenStreamByIdentifier(null, "parameter")));

    assertSimpleParameter(parameter, "name", "subject");
    assertSimpleParameter(parameter, "type", "STRING");
    assertSimpleParameter(parameter, "optional", false);
    assertSimpleParameter(parameter, "description", "The name of the person you want to greet");

    ComponentAst payloadType = singleNode(operation.directChildrenStreamByIdentifier(null, "output")
            .flatMap(c -> c.directChildrenStreamByIdentifier(null, "payload-type")));

    assertThat(payloadType.getComponentType(), is(OUTPUT_PAYLOAD_TYPE));
    assertSimpleParameter(payloadType, "type", "STRING");

    ComponentAst body = singleNode(operation.directChildrenStreamByIdentifier(null, "body"));
    assertThat(body.getModel(NestedChainModel.class).isPresent(), is(true));
    assertThat(body.directChildrenStream().allMatch(c -> c.getComponentType().equals(OPERATION)), is(true));
  }

  private <T> T singleNode(Stream<T> stream) {
    List<T> list = stream.collect(toList());
    assertThat(list, hasSize(1));

    return list.get(0);
  }

  private void assertSimpleParameter(ComponentAst component, String paramName, Object expected) {
    ComponentParameterAst parameter = component.getParameter(paramName);
    assertThat("Parameter " + paramName + " not found", parameter, is(notNullValue()));
    assertThat(parameter.getValue().getRight(), equalTo(expected));
  }

  private ComponentAst findOperationByName(ArtifactAst importAst, String name) {
    return singleNode(importAst.topLevelComponentsStream()
            .filter(c -> c.getIdentifier().getName().equals("def") && name.equals(c.getParameter("name").getResolvedRawValue())));
  }
}
