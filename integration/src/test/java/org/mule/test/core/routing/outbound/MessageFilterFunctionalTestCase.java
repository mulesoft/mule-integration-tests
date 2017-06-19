/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.routing.outbound;

import static org.mule.functional.api.component.FlowAssert.verify;

import org.junit.Test;

import org.mule.test.AbstractIntegrationTestCase;

public class MessageFilterFunctionalTestCase extends AbstractIntegrationTestCase {

  @Test
  public void testFlowCallerStopsAfterUnacceptedEvent() throws Exception {
    runFlow("MainFlow");
    verify();
  }

  @Override
  protected String getConfigFile() {
    return "message-filter-config.xml";
  }

}
