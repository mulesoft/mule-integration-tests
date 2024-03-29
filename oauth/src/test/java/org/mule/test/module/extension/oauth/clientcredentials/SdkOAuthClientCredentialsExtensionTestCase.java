/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

public class SdkOAuthClientCredentialsExtensionTestCase extends OAuthClientCredentialsExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-extension-config.xml", "client-credentials-flows.xml"};
  }

}
