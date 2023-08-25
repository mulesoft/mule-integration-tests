/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.junit.Assert.fail;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_SECRET;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ORG_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;
import static org.mule.test.allure.AllureConstants.OauthFeature.OCS_SUPPORT;
import static org.mule.test.allure.AllureConstants.OauthFeature.OcsStory.OCS_CONNECTION_VALIDATION;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OCS_SUPPORT)
@Story(OCS_CONNECTION_VALIDATION)
public class PlatformManagedOAuthMissingRequiredParameterTestCase extends PlatformManagedOAuthNegativeTestCase {

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

  @Override
  protected Map<String, Object> getDescriptorParameters() {
    return new HashMap<>();
  }

  @Override
  protected void addExceptionAssertions(ExpectedException expectedException) {
    expectedException.expectCause(hasMessage("Parameter 'display' is required but was not found"));
  }

  @Override
  @Test
  @Description("This test checks the validation done on the data fetched from OCS, not on data provided in the DSL.")
  public void failedSetup() {
    fail("Config should have failed to parse");
  }


}

