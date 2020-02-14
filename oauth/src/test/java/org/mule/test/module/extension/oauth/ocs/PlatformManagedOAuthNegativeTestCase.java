/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

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
  public void fail() {
    Assert.fail("Config should have failed to parse");
  }

}
