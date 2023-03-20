/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(ROUTERS)
@Story(ASYNC)
public class AsyncWithGlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty propagateDisposeError = new SystemProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR, "");

  @Override
  protected String getConfigFile() {
    return "routers/async-global-error-handler.xml";
  }

  @Test
  @Issue("W-12556497")
  public void asyncWithErrorHandling() throws Exception {
    flowRunner("asyncWithGlobalErrorHandling").run();
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}

