/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OAuthClientCredentialsInvalidConfigExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-invalid-config-extension-config.xml"};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(LifecycleException.class);
    expectedException.expectMessage("The uri provided 'no.scheme.url.com' must contain a scheme.");
    super.doSetUpBeforeMuleContextCreation();
  }

  @Test
  public void tokenUrlWithNoScheme() throws Exception {
    fail("This test must throw an error initializing the app, so it should not get to this line.");
  }

}
