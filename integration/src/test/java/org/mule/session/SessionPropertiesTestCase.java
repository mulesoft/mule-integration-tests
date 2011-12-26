/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;

import org.junit.Test;

public class SessionPropertiesTestCase extends org.mule.tck.junit4.FunctionalTestCase
{
    @Test
    public void setSessionPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("A");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessageAsString());
    }

    @Test
    public void setSessionPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("B");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
    }

    @Test
    public void propagateSessionPropertyOverTransportRequestResponse() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct(
            "RequestResponseSessionPropertySettingChain");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", result.getMessage().getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", result.getMessage().getProperty("key2", PropertyScope.SESSION));
        assertEquals("value3", result.getMessage().getProperty("key3", PropertyScope.SESSION));
        assertEquals("value4", result.getMessage().getProperty("key4", PropertyScope.SESSION));
        assertEquals("value5", result.getMessage().getProperty("key5", PropertyScope.SESSION));
        assertEquals(nonSerializable,
            result.getMessage().getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    @Test
    public void propagateSessionPropertyOverTransportOneWay() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry()
            .lookupFlowConstruct("OneWaySessionPropertySettingChain");
        flowA.process(event);

        MuleMessage out = muleContext.getClient().request("vm://H-out", RECEIVE_TIMEOUT);

        assertNotNull(out);
        assertEquals("value", out.getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", out.getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", out.getProperty("key2", PropertyScope.SESSION));
        assertNull(out.getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    @Test
    public void nonSerializableSessionPropertyOneWayFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("PassthroughFlow");
        flow.process(event);

        MuleMessage out = muleContext.getClient().request("vm://PassthroughFlow-out", RECEIVE_TIMEOUT);

        assertNotNull(out);
        assertNull(out.getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void flowRefSessionPropertyPropagation() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("FlowRefWithSessionProperties");
        MuleEvent result = flow.process(event);

        assertSame(event.getSession(), result.getSession());

        assertNotNull(result);
        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", result.getMessage().getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", result.getMessage().getProperty("key2", PropertyScope.SESSION));
        assertEquals(nonSerializable,
            result.getMessage().getProperty("keyNonSerializable", PropertyScope.SESSION));

    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/session/session-properties-config.xml";
    }

}
