/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.connection;

import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RetryTemplateStory.RETRY_TEMPLATE;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(RECONNECTION_POLICIES)
@Story(RETRY_TEMPLATE)
@RunnerDelegateTo(Parameterized.class)
public class ReconnectionTestCase extends AbstractIntegrationTestCase { 

  private String flow;

  public ReconnectionTestCase(String flow) {
    this.flow = flow;
  }

  @Parameters(name = "flow: {0}")
  public static List<String> parameters() {
    return Arrays.asList("reconnectionTest", "reconnectionWithDynamicConfigTest", "noReconnectionTest",
                         "noReconnectionWithDynamicConfigTest");
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/connection/reconnection-config.xml";
  }

  @Test
  public void executedOperationThrowsConnectivityError() throws Exception {
    testReconnectionFlow(flow);
  }

  private void testReconnectionFlow(String flowName) throws Exception {
    flowRunner(flowName)
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));
  }
}
