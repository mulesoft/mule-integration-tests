/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import javax.inject.Inject;

import org.junit.Test;

public class ScopeLifecycleTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LifecycleTrackerRegistry trackerRegistry;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/scope.xml";
  }

  @Test
  public void scopeLifecycle() throws Exception {
    // Run the flow so the scope is initialized.
    flowRunner("flow").run();
    assertThat(trackerRegistry.get("scope").getCalledPhases(), is(asList("setMuleContext", "initialise", "start")));
  }

}
