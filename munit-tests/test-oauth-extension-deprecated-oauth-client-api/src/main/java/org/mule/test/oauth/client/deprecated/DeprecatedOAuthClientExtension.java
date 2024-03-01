/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.client.deprecated;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Extension(name = "Deprecated OAuth Client API Test Extension")
@Configurations({TestOAuthConfig.class})
@Xml(prefix = "test-deprecated-oauth-client")
public class DeprecatedOAuthClientExtension {


}
