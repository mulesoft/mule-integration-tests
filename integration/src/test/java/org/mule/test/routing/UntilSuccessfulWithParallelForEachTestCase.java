/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
@Issue("W-14011209")
public class UntilSuccessfulWithParallelForEachTestCase extends AbstractIntegrationTestCase {

  public static final String STARTING_FLOW = "startingFlow";
  public static final String RESPONSE_PAYLOAD = "OK";

  @Override
  protected String getConfigFile() {
    return "until-successful-with-parallel-foreach.xml";
  }

  @Test
  public void testConcurrentUntilSuccessfulAfterAnUntilSuccessful() throws Exception {
    CoreEvent muleEvent = flowRunner(STARTING_FLOW).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(RESPONSE_PAYLOAD));
  }
}
