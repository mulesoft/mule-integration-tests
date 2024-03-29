/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
