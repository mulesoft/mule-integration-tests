/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.messaging.meps;

import org.mule.api.context.notification.ServerNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

// START SNIPPET: full-class
public class InOnlyTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Only.xml";
    }

    public void testExchange() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        final Latch latch = new Latch();
        client.getMuleContext().registerListener(new FunctionalTestNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                latch.countDown();
            }
        });

        client.dispatch("inboundEndpoint", "some data", null);
        assertTrue(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
// END SNIPPET: full-class
