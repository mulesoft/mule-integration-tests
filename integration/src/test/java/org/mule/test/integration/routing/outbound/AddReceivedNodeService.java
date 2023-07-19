/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.routing.outbound;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * TODO
 */
public class AddReceivedNodeService {

  public Document addNodeTo(Document doc) {
    doc.getRootElement().addElement("Received");

    return doc;
  }

  public Element addNodeTo(Element doc) {
    doc.addElement("Received");

    return doc;
  }

  public org.w3c.dom.Document addNodeTo(org.w3c.dom.Document doc) {
    doc.appendChild(doc.createElement("Received"));
    return doc;
  }

  public org.w3c.dom.Element addNodeTo(org.w3c.dom.Element element) {
    element.appendChild(element.getOwnerDocument().createElement("Received"));
    return element;
  }

}
