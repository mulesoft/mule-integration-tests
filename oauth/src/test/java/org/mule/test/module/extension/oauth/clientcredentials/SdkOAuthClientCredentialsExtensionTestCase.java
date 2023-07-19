/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

public class SdkOAuthClientCredentialsExtensionTestCase extends OAuthClientCredentialsExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-extension-config.xml", "client-credentials-flows.xml"};
  }

}
