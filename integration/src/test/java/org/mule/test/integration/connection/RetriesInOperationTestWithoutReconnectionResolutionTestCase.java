/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.connection;

import static org.mule.runtime.api.util.MuleSystemProperties.RESOLVE_OPERATION_EXECUTION_WITHOUT_USING_RECONNECTION_CONFIG_PROPERTY;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RECONNECTION_POLICIES;
import static org.mule.test.allure.AllureConstants.ReconnectionPolicyFeature.RetryTemplateStory.RETRY_TEMPLATE;

import org.junit.Rule;
import org.junit.runners.Parameterized;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(RECONNECTION_POLICIES)
@Story(RETRY_TEMPLATE)
@RunnerDelegateTo(Parameterized.class)
public class RetriesInOperationTestWithoutReconnectionResolutionTestCase extends ReconnectionTestCase {

  @Rule
  public SystemProperty workingDirSysProp =
      new SystemProperty(RESOLVE_OPERATION_EXECUTION_WITHOUT_USING_RECONNECTION_CONFIG_PROPERTY, "true");

  public RetriesInOperationTestWithoutReconnectionResolutionTestCase(String flow) {
    super(flow);
  }

}
