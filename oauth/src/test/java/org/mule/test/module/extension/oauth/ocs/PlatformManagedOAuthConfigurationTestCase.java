/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_SECRET;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ORG_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.ClassRule;

public abstract class PlatformManagedOAuthConfigurationTestCase extends PlatformManagedOAuthTestCase {

  @ClassRule
  public static SystemProperty ocsServiceUrl = new SystemProperty(OCS_SERVICE_URL, "testServiceUrl");
  @ClassRule
  public static SystemProperty ocsPlatformAuthUrl = new SystemProperty(OCS_PLATFORM_AUTH_URL, "testPlatformAuthUrl");
  @ClassRule
  public static SystemProperty ocsClientId = new SystemProperty(OCS_CLIENT_ID, "testClientId");
  @ClassRule
  public static SystemProperty ocsClientSecret = new SystemProperty(OCS_CLIENT_SECRET, "testClientSecret");
  @ClassRule
  public static SystemProperty ocsOrgId = new SystemProperty(OCS_ORG_ID, "testOrgId");

}
