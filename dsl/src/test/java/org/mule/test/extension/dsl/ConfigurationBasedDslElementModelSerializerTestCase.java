/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.XmlDslElementModelConverter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;

public class ConfigurationBasedDslElementModelSerializerTestCase extends AbstractElementModelTestCase {

  public static final int SOCKETS_SEND_RECEIVE_PATH = 5;
  public static final int TRY_PATH = 6;
  public static final int WSC_CONSUME_PATH = 7;

  private Element flow;
  private String expectedAppXml;

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public DynamicPort otherPort = new DynamicPort("otherPort");

  @Before
  public void createDocument() throws Exception {
    applicationModel = loadApplicationModel();
    createAppDocument();

    Element flow = doc.createElement("flow");
    flow.setAttribute("name", "testFlow");
    flow.setAttribute("initialState", "stopped");
    this.flow = flow;
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(getConfigFile(), getClass());
  }

  @Override
  protected String getConfigFile() {
    return "component-config-app-declaration.xml";
  }

  @Test
  @Ignore("MULE-20013")
  public void serialize() throws Exception {
    XmlDslElementModelConverter converter = XmlDslElementModelConverter.getDefault(this.doc);

    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, DB_CONFIG))));
    final Element httpListenerConfig = converter.asXml(resolve(getAppElement(applicationModel, HTTP_LISTENER_CONFIG)));
    // TODO MULE-17372 AST must allow to access the raw property placeholder and not just the resolved value
    httpListenerConfig.getElementsByTagName("http:listener-connection").item(0)
        .getAttributes().getNamedItem("port")
        .setNodeValue("${port}");
    doc.getDocumentElement().appendChild(httpListenerConfig);
    final Element httpRequesterConfig = converter.asXml(resolve(getAppElement(applicationModel, HTTP_REQUESTER_CONFIG)));
    // TODO MULE-17372 AST must allow to access the raw property placeholder and not just the resolved value
    httpRequesterConfig.getElementsByTagName("http:request-connection").item(0)
        .getAttributes().getNamedItem("port")
        .setNodeValue("${otherPort}");
    doc.getDocumentElement().appendChild(httpRequesterConfig);
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, "sockets-config"))));
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, "wsc-config"))));

    ComponentConfiguration componentsFlow = getAppElement(applicationModel, COMPONENTS_FLOW);
    Element httpListenerSource = converter.asXml(resolve(componentsFlow.getNestedComponents().get(LISTENER_PATH)));

    // For some reason mule provides the `redelivery-policy` as an external component, but we need to serialize it
    // as an http child element to match the original application
    addRedeliveryPolicy(componentsFlow, httpListenerSource);
    flow.appendChild(httpListenerSource);

    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(DB_BULK_INSERT_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(REQUESTER_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(DB_INSERT_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(SOCKETS_SEND_RECEIVE_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(TRY_PATH))));
    DslElementModel<NamedObject> resolve = resolve(componentsFlow.getNestedComponents().get(WSC_CONSUME_PATH));
    Element element = converter.asXml(resolve);
    flow.appendChild(element);

    doc.getDocumentElement().appendChild(flow);

    String serializationResult = write();

    compareXML(expectedAppXml, serializationResult);
  }

  private void addRedeliveryPolicy(ComponentConfiguration componentsFlow, Element httpListenerSource) {
    ComponentConfiguration redeliveryPolicy = componentsFlow.getNestedComponents().get(1);
    Element policyElement = doc.createElement(REDELIVERY_POLICY_ELEMENT_IDENTIFIER);
    redeliveryPolicy.getParameters().forEach(policyElement::setAttribute);
    httpListenerSource.insertBefore(policyElement, httpListenerSource.getFirstChild());
  }

}
