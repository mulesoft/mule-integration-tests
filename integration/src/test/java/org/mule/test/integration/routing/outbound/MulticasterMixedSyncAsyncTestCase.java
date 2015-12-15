/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.List;

import org.junit.Test;

public class MulticasterMixedSyncAsyncTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/multicaster-mixed-sync-async-test-flow.xml";
    }

    @Test
    public void testMixedMulticast() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://distributor.queue", new Apple(), null);

        assertNotNull(result);
        assertTrue(result.getPayload() instanceof List);
        List<String> results = ((List<MuleMessage>) result.getPayload()).stream().map(msg -> (String) msg.getPayload
                ()).collect(toList());
        assertEquals(2, results.size());

        //ServiceTwo endpoint is async
        assertTrue(results.contains("Apple Received in ServiceOne"));
        assertTrue(results.contains("Apple Received in ServiceThree"));
    }
}
