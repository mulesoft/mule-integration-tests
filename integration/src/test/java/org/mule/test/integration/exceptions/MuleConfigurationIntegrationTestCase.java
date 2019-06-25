/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

@Issue("MULE-17051")
public class MuleConfigurationIntegrationTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler-config-default.xml";
  }

  @Test
  public void modifyingDefaultErrorHandlerCanBeDeployed() {
    // only need to check that configuration can actually be deployed
  }
}
