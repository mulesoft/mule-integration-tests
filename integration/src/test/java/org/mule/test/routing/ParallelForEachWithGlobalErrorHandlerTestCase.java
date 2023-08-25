/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.routing;

import static org.mule.test.allure.AllureConstants.RoutersFeature.ParallelForEachStory.PARALLEL_FOR_EACH;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;

import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ROUTERS)
@Story(PARALLEL_FOR_EACH)
public class ParallelForEachWithGlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "routers/parallel-for-each-global-error-handler.xml";
  }

  @Test(expected = Exception.class)
  @Issue("W-12556497")
  public void parallelForEachWithErrorHandling() throws Exception {
    flowRunner("parallelForEachWithGlobalErrorHandling").run();
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
