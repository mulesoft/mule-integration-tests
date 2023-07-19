/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

import org.junit.Before;

public class StaticOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"static-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }
}
