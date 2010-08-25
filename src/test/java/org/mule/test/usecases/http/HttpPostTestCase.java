/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.http;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class HttpPostTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-post.xml";
    }

    public void testPost() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("httpRequest", "payload", null);
        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        assertEquals("IncidentData=payload", message.getPayloadAsString());
    }

}
