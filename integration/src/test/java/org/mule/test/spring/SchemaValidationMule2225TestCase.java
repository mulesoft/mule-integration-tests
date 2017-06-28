/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Note: this test will fail if off-line.
 */
public class SchemaValidationMule2225TestCase extends AbstractMuleTestCase {

  @Test
  public void testValidation() throws SAXException, IOException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
    Source muleXsd = new StreamSource(load("META-INF/mule.xsd"));
    Schema schema = schemaFactory.newSchema(muleXsd);
    Source muleRootTestXml = new StreamSource(load("org/mule/test/spring/mule-root-test.xml"));
    schema.newValidator().validate(muleRootTestXml);
  }

  protected InputStream load(String name) throws IOException {
    InputStream stream = IOUtils.getResourceAsStream(name, getClass());
    assertNotNull("Cannot load " + name, stream);
    return stream;
  }

}
