/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.XmlDslElementModelConverter;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;

public class ConfigurationBasedDslElementModelSerializerTestCase extends AbstractElementModelTestCase {

  public static final int SOCKETS_SEND_RECEIVE_PATH = 4;
  public static final int TRY_PATH = 5;
  public static final int WSC_CONSUME_PATH = 6;

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

    ComponentAst componentsFlow = getAppElement(applicationModel, COMPONENTS_FLOW);
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(LISTENER_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(DB_BULK_INSERT_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(REQUESTER_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(DB_INSERT_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(SOCKETS_SEND_RECEIVE_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(TRY_PATH).findFirst().get())));
    flow.appendChild(converter
        .asXml(resolve(componentsFlow.directChildrenStream().skip(WSC_CONSUME_PATH).findFirst().get())));

    doc.getDocumentElement().appendChild(flow);

    String serializationResult = write();

    compareXML(expectedAppXml, serializationResult);
  }

}
