/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

public class SdkOAuthExtensionTestCase extends OAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"sdk-auth-code-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

}
