/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.validation;

import static org.hamcrest.Matchers.containsString;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.integration.AbstractConfigurationFailuresTestCase;

public class FlowConfigurationFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void errorHandlerCantHaveOnErrorWithoutTypeOrExpression() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("Invalid global element name 'flow/myFlow' in org/mule/test/integration/validation/invalid-flow-name-config.xml:7. Problem is: Invalid character used in location. Invalid characters are /,[,],{,},#"));
    loadConfiguration("org/mule/test/integration/validation/invalid-flow-name-config.xml");
  }

}
