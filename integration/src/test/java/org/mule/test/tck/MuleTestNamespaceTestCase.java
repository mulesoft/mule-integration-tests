/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;

import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MuleTestNamespaceTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "test-namespace-config-flow.xml";
  }

  @Test
  public void testComponent1Config() throws Exception {
    FunctionalTestProcessor ftc = getFromFlow(locator, "testService1");

    assertFalse(ftc.isEnableMessageHistory());
    assertFalse(ftc.isEnableNotifications());
    assertNull(ftc.getAppendString());
    assertEquals("Foo Bar Car Jar", ftc.getReturnData());
    assertNotNull(ftc.getEventCallback());
    assertTrue(ftc.getEventCallback() instanceof TestCallback);
  }

  @Test
  public void testComponent3Config() throws Exception {
    FunctionalTestProcessor ftc = getFromFlow(locator, "testService3");

    assertFalse(ftc.isEnableMessageHistory());
    assertTrue(ftc.isEnableNotifications());
    assertEquals("#[mel:context:serviceName]", ftc.getAppendString());
    assertNull(ftc.getReturnData());
    assertNull(ftc.getEventCallback());
  }

  public static final class TestCallback extends AbstractComponent implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      // Nothing to do
    }

  }
}
