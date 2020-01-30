/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class PlatformManagedOAuthConfigurationTestCase extends PlatformManagedOAuthTestCase {

  @Inject
  private ConfigurationProperties configurationProperties;

  //  @Override
  //  protected String[] getConfigFiles() {
  //    return new String[] {"ocs/platform-managed-config.xml"};
  //  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-extension-config.xml", "client-credentials-flows.xml"};
  }

  @Test
  public void testApplicationProperties() {
    int a = 2;
  }

}
