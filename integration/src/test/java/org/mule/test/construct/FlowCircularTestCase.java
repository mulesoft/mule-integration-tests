/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowCircularTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-circular-config.xml";
  }

  @Test
  public void circularFlowRefsDontFailParsing() throws Exception {}
}
