/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.routing.outbound;

import static org.mule.functional.api.component.FlowAssert.verify;
import static org.mule.runtime.core.processor.AbstractFilteringMessageProcessor.FILTER_ON_UNACCEPTED_NOT_STOP_PARENT_FLOW;

import org.junit.Rule;
import org.junit.Test;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

public class MessageFilterCompatibilityFunctionalTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty(FILTER_ON_UNACCEPTED_NOT_STOP_PARENT_FLOW, "true");

  @Override
  protected String getConfigFile() {
    return "message-filter-compatibility-config.xml";
  }

  @Test
  public void testFlowCallerStopsAfterUnacceptedEvent() throws Exception {
    runFlow("MainFlow");
    verify();
  }

}
