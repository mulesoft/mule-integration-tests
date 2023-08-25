/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.junit.Assert.fail;

import org.mule.runtime.api.lifecycle.InitialisationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class PlatformManagedOAuthNegativeTestCase extends PlatformManagedOAuthTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  public void doSetUpBeforeMuleContextCreation() throws Exception {
    configureExceptionAssertions();
    super.doSetUpBeforeMuleContextCreation();
  }

  private void configureExceptionAssertions() {
    addExceptionAssertions(expectedException);
  }

  protected void addExceptionAssertions(ExpectedException expectedException) {}

  @Test
  public void failedSetup() {
    fail("Config should have failed to parse");
  }

}
