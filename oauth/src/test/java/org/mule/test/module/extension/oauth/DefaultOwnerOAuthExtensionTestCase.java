/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth;

import org.junit.Before;

public class DefaultOwnerOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"default-owner-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Before
  public void setOwnerId() {
    ownerId = DEFAULT_OWNER_ID;
  }

}
