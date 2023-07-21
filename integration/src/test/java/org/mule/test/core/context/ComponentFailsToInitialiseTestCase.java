/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ComponentFailsToInitialiseTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(CoreMatchers.<InitialisationException>instanceOf(InitialisationException.class));
    FailLifecycleTestObject.setup();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/component-fail-initialise.xml";
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    assertThat(FailLifecycleTestObject.isInitInvoked(), is(true));
    assertThat(FailLifecycleTestObject.isDisposeInvoked(), is(false));
  }

  @Test
  public void failToInitialise() throws Exception {}
}
