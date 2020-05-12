/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;

import org.mule.runtime.config.api.dsl.model.XmlDslElementModelConverter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class ConfigurationBasedDslElementModelSerializerTestCase extends AbstractElementModelTestCase {

  public static final int SOCKETS_SEND_RECEIVE_PATH = 4;
  public static final int TRY_PATH = 5;
  public static final int WSC_CONSUME_PATH = 6;

  private Element flow;
  private String expectedAppXml;

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
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, HTTP_LISTENER_CONFIG))));
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, HTTP_REQUESTER_CONFIG))));
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, "sockets-config"))));
    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, "wsc-config"))));

    ComponentConfiguration componentsFlow = getAppElement(applicationModel, COMPONENTS_FLOW);
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(LISTENER_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(DB_BULK_INSERT_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(REQUESTER_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(DB_INSERT_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(SOCKETS_SEND_RECEIVE_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(TRY_PATH))));
    flow.appendChild(converter.asXml(resolve(componentsFlow.getNestedComponents().get(WSC_CONSUME_PATH))));

    doc.getDocumentElement().appendChild(flow);

    String serializationResult = write();

    compareXML(expectedAppXml, serializationResult);
  }

}
