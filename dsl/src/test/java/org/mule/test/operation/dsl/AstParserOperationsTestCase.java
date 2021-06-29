/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation.dsl;

import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.CoreRuntimeExtensionModelProvider;
import org.mule.runtime.core.api.extension.OperationDslExtensionModelProvider;

import java.util.HashMap;
import java.util.Map;

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
    importAst.topLevelComponentsStream().filter(c -> c.getComponentType())
    importAst.toString();
  }
}
