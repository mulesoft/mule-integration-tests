/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

public class XsltWithParamsTransformerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-namespace-test.xml";
    }

    public void testTransformWithParameter() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("test1");
        assertNotNull(trans);
        MuleMessage message = new DefaultMuleMessage("<testing/>", muleContext);
        message.setOutboundProperty("Welcome", "hello");
        Object result = trans.transform(message);
        assertNotNull(result);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><echo-value xmlns=\"http://test.com\">hello</echo-value>", result);
    }
}
