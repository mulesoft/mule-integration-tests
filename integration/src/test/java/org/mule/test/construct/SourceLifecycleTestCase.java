/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.functional.api.component.LifecycleTrackerSource;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.inject.Inject;

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
