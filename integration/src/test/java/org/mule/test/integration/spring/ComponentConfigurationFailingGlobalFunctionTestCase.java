/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ComponentConfigurationFailingGlobalFunctionTestCase extends AbstractIntegrationTestCase {

  private String EXPECTED_PAYLOAD = "Test payload";

  @Override
  public String getConfigFile() {
    return "org/mule/test/integration/spring/sampleapp-with-global-fn.xml";
  }

  @Test
  public void runFlowWithoutException() throws Exception {
    CoreEvent event = flowRunner("main-flow").run();
    String msg = (String) event.getMessage().getPayload().getValue();
    assertEquals(EXPECTED_PAYLOAD, msg);
  }
}
