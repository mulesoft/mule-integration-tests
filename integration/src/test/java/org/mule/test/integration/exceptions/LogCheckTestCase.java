/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;
import static org.junit.Assert.fail;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.test.AbstractIntegrationTestCase;


import java.util.Collection;

import org.junit.Test;

public class LogCheckTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-config.xml";
  }

  @Test
  public void assertAllFlows() throws Exception{
    Collection<FlowConstruct> flows = muleContext.getRegistry().lookupFlowConstructs();
    for ( FlowConstruct flow : flows) {
      flowRunner(flow.getName()).run();
    }
  }


}
