/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.OauthFeature.SDK_OAUTH_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ConnectivityTestingStory.CONNECTIVITY_TESTING_SERVICE;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(SDK_OAUTH_SUPPORT), @Feature(SDK_TOOLING_SUPPORT)})
@Story(CONNECTIVITY_TESTING_SERVICE)
public class OAuthConnectivityValidationRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-validated-connection-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Inject
  @Named(CONNECTIVITY_TESTING_SERVICE_KEY)
  protected ConnectivityTestingService connectivityTestingService;

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Test
  public void refreshTokenOnConnectionValidation() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubRefreshToken();

    ConnectionValidationResult connectionValidationResult = testConnection();
    assertThat(connectionValidationResult.isValid(), is(true));
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshedTokenAlreadyExpiredOnConnectionValidation() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpiredOnce();

    ConnectionValidationResult connectionValidationResult = testConnection();
    verifyTokenRefreshedTwice();
    assertThat(connectionValidationResult.isValid(), is(true));
  }

  @Test
  public void refreshedTokenAlreadyExpiredTwiceOnConnectionValidation() throws Exception {
    simulateCallback();

    stubRefreshedTokenAlreadyExpiredTwice();

    ConnectionValidationResult connectionValidationResult = testConnection();
    verifyTokenRefreshedTwice();
    assertThat(connectionValidationResult.isValid(), is(false));

    expectExpiredTokenException();
    throw connectionValidationResult.getException();
  }

  private ConnectionValidationResult testConnection() {
    return connectivityTestingService.testConnection(Location.builder().globalName("oauth").build());
  }
}

