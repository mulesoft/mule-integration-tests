/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Before;

public class DefaultOwnerOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"default-owner-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() {
    ownerId = BaseOAuthExtensionTestCase.DEFAULT_OWNER_ID;
  }

  @Override
  protected String getCustomOwnerId() {
    return null;
  }
}
