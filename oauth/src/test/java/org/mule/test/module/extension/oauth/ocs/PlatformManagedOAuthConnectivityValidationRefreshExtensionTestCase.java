/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.OauthFeature.OCS_SUPPORT;
import static org.mule.test.allure.AllureConstants.OauthFeature.OcsStory.OCS_CONNECTION_VALIDATION;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ConnectivityTestingStory.CONNECTIVITY_TESTING_SERVICE;
import static org.mule.test.oauth.TestOAuthRefreshValidationProvider.TIMES_REFRESH_IS_NEEDED;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Features({@Feature(SDK_TOOLING_SUPPORT), @Feature(OCS_SUPPORT)})
@Stories({@Story(CONNECTIVITY_TESTING_SERVICE), @Story(OCS_CONNECTION_VALIDATION)})
public class PlatformManagedOAuthConnectivityValidationRefreshExtensionTestCase
    extends PlatformManagedOAuthConfigurationTestCase {

  private static final String OAUTH_PLATFORM_CONFIG = "oauth-platform";

  @Inject
  @Named(CONNECTIVITY_TESTING_SERVICE_KEY)
  protected ConnectivityTestingService connectivityTestingService;

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"With refresh validation configuration", "ocs/with-refresh-validation-platform-managed-config.xml", true}
    });
  }

  @Before
  public void resetCounter() throws Exception {
    TIMES_REFRESH_IS_NEEDED = 0;
  }

  @Test
  public void refreshTokenOnConnectionValidation() throws Exception {
    TIMES_REFRESH_IS_NEEDED = 1;

    ConnectionValidationResult connectionValidationResult = testConnection();
    assertThat(connectionValidationResult.isValid(), is(true));
    verify(mockPlatformDancer, times(1)).refreshToken();
  }

  @Test
  public void refreshedTokenAlreadyExpiredOnConnectionValidation() throws Exception {
    TIMES_REFRESH_IS_NEEDED = 2;

    ConnectionValidationResult connectionValidationResult = testConnection();
    assertThat(connectionValidationResult.isValid(), is(true));
    verify(mockPlatformDancer, times(2)).refreshToken();
  }

  @Test
  public void refreshedTokenAlreadyExpiredTwiceOnConnectionValidation() throws Exception {
    TIMES_REFRESH_IS_NEEDED = 3;

    ConnectionValidationResult connectionValidationResult = testConnection();
    assertThat(connectionValidationResult.isValid(), is(false));
    verify(mockPlatformDancer, times(2)).refreshToken();

    expectExpiredTokenException();
    throw connectionValidationResult.getException();
  }

  private ConnectionValidationResult testConnection() {
    return connectivityTestingService.testConnection(Location.builder().globalName(OAUTH_PLATFORM_CONFIG).build());
  }

}
