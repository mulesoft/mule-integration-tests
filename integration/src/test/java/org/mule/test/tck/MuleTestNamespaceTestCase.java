/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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

}
