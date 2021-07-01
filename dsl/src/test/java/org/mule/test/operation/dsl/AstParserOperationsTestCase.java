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
  }

  @Test
  public void parseHelloOperation() {
    final ArtifactAst importAst = parser.parse(classLoader.getResource("org/mule/test/config/operation/mule-operations.xml"));
    ComponentAst helloOp = findOperationByName(importAst, "hello");
    assertThat(helloOp.getComponentType(), equalTo(OPERATION_DEF));
    assertThat(helloOp.getParameter("public").getValue().getRight(), is(false));

    assertThat(helloOp.getModel(ConstructModel.class).isPresent(), is(true));
    ComponentAst parameter  = singleNode(helloOp.directChildrenStreamByIdentifier(null, "parameters")
            .flatMap(c -> c.directChildrenStreamByIdentifier(null, "parameter")));

    assertSimpleParameter(parameter, "name", "subject");
    assertSimpleParameter(parameter, "type", "STRING");
    assertSimpleParameter(parameter, "optional", false);
    assertSimpleParameter(parameter, "description", "The name of the person you want to greet");

    ComponentAst payloadType = singleNode(helloOp.directChildrenStreamByIdentifier(null, "output")
            .flatMap(c -> c.directChildrenStreamByIdentifier(null, "payload-type")));

    assertThat(payloadType.getComponentType(), is(OUTPUT_PAYLOAD_TYPE));
    assertSimpleParameter(payloadType, "type", "STRING");

    ComponentAst body = singleNode(helloOp.directChildrenStreamByIdentifier(null, "body"));
    assertThat(body.getModel(NestedChainModel.class).isPresent(), is(true));
    assertThat(body.directChildrenStream().allMatch(c -> c.getComponentType().equals(OPERATION)), is(true));
  }

  private <T> T singleNode(Stream<T> stream) {
    List<T> list = stream.collect(toList());
    assertThat(list, hasSize(1));

    return list.get(0);
  }

  private void assertSimpleParameter(ComponentAst component, String paramName, Object expected) {
    assertThat(component.getParameter(paramName).getValue().getRight(), equalTo(expected));
  }

  private ComponentAst findOperationByName(ArtifactAst importAst, String name) {
    return singleNode(importAst.topLevelComponentsStream()
            .filter(c -> c.getIdentifier().getName().equals("def") && name.equals(c.getParameter("name").getResolvedRawValue())));
  }
}
