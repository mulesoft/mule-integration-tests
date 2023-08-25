/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;

import org.mule.functional.api.component.FunctionalTestProcessor;
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

}
