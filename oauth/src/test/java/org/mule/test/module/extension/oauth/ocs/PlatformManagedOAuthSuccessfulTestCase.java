/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.test.oauth.TestOAuthConnection;
import java.util.Optional;

import org.junit.Test;

public class PlatformManagedOAuthSuccessfulTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final String EMPTY_STRING = "";
  private static final String CONFIG_NAME = "oauth-platform";

  @Test
  public void accessTokenRetrieval() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().getState().getAccessToken(), is(TEST_ACCESS_TOKEN));
  }

  @Test
  public void correctOAuthStateInjected() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    if (usesAuthorizationCodeProvider) {
      assertThat(connection.getState().getState(), is(instanceOf(AuthorizationCodeState.class)));
    } else {
      assertThat(connection.getState().getState(), is(instanceOf(ClientCredentialsState.class)));
    }
  }

  @Test
  public void noRefreshTokenIsRetrieved() throws Exception {
    if (usesAuthorizationCodeProvider) {
      TestOAuthConnection connection =
          (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
      assertThat(((AuthorizationCodeState) connection.getState().getState()).getRefreshToken(), is(Optional.empty()));
    }
  }

  @Test
  public void tokenAndAccessUrlsAreNotDisclosed() throws Exception {
    if (usesAuthorizationCodeProvider) {
      TestOAuthConnection connection =
          (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
      assertThat(((AuthorizationCodeState) connection.getState().getState()).getAccessTokenUrl(), is(EMPTY_STRING));
      assertThat(((AuthorizationCodeState) connection.getState().getState()).getAuthorizationUrl(), is(EMPTY_STRING));
    }
  }

  @Test
  public void callbackParametersAreSettedCorrectly() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().getInstanceId(), is(INSTANCE_ID_TEST));
    assertThat(connection.getState().getUserId(), is(USER_ID_TEST));
  }

  @Test
  public void expiredInIsRetrieved() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().getState().getExpiresIn().get(), is(EXPIRES_IN_TEST));
  }

  @Test
  public void parametersAreInjectedCorrectly() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().getApiVersion(), is(API_VERSION_TEST));
    assertThat(connection.getState().isPrompt(), is(PROMPT_TEST));
  }

  @Test
  public void configurationNameIsInjectedCorrectly() throws Exception {
    TestOAuthConnection connection = (TestOAuthConnection) flowRunner("getConnection").run().getMessage().getPayload().getValue();
    assertThat(connection.getState().getConfigName(), is(CONFIG_NAME));
  }

}
