/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;
import static org.mule.test.allure.AllureConstants.OauthFeature.OCS_SUPPORT;
import static org.mule.test.allure.AllureConstants.OauthFeature.OcsStory.OCS_CONNECTION_VALIDATION;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.ClassRule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OCS_SUPPORT)
@Story(OCS_CONNECTION_VALIDATION)
@RunnerDelegateTo(Parameterized.class)
public class PlatformManagedOAuthConfigurationMissingTestCase extends PlatformManagedOAuthNegativeTestCase {

  @ClassRule
  public static SystemProperty ocsServiceUrl = new SystemProperty(OCS_SERVICE_URL, "testServiceUrl");
  @ClassRule
  public static SystemProperty ocsPlatformAuthUrl = new SystemProperty(OCS_PLATFORM_AUTH_URL, "testPlatformAuthUrl");
  @ClassRule
  public static SystemProperty ocsClientId = new SystemProperty(OCS_CLIENT_ID, "testClientId");

  @Override
  protected void addExceptionAssertions(ExpectedException expectedException) {
    expectedException.expect(InitialisationException.class);
    expectedException
        .expectCause(hasCause(hasCause(hasMessage("OCS property 'ocs.service.client.secret' has not been set"))));
  }
}
