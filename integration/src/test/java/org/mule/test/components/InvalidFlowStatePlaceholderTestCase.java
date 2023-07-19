/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components;

import org.mule.test.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public class InvalidFlowStatePlaceholderTestCase extends AbstractConfigurationErrorTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/invalid-flow-initial-state.xml";
  }

  @Test
  public void invalidInitialFlowStatePlaceholder() throws Exception {
    assertConfigurationError("'${state}]' is not a valid value of union type '#AnonType_initialStateflowType'");
  }

}
