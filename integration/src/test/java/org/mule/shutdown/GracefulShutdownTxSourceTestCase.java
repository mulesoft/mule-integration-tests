/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class GracefulShutdownTxSourceTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty propagateDisposeError = new SystemProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR, "");

  private TestConnectorQueueHandler queueHandler;

  @Before
  public void before() {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/shutdown/flow-with-tx-source.xml";
  }

  @Test
  @Description("Verify that the graceful shutdown occurs in a timely manner")
  public void flowStopTimelyManner() {
    assertThat(queueHandler.read("txFlowRan", RECEIVE_TIMEOUT), not(nullValue()));
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
