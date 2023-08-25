/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.LifecycleTrackerSource;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SourceLifecycleTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LifecycleTrackerRegistry trackerRegistry;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/source.xml";
  }

  @Before
  public void before() {
    LifecycleTrackerSource.clearSources();
  }

  @After
  public void after() {
    LifecycleTrackerSource.clearSources();
  }

  @Test
  public void sourceLifecycle() throws Exception {
    assertThat(trackerRegistry.get("source").getCalledPhases(), is(asList("setMuleContext", "initialise", "start")));
  }

}
