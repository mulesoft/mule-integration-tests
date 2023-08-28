/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class MultipleOwnersTestCase extends BaseOAuthExtensionTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-config.xml", "oauth-extension-flows.xml"};
  }

  @Test
  @Issue("MULE-20019")
  @Description("Tests that only the credentials of the corresponding resource owner are updated during " +
      "authorization, token refresh and token invalidation")
  public void multipleResourceOwners() throws Exception {
    ResourceOwnerData firstOwner = new ResourceOwnerData("firstOwnerId", "1stOwnerAccessToken");
    ResourceOwnerData secondOwner = new ResourceOwnerData("secondOwnerId", "2ndOwnerAccessToken");

    simulateCallback(firstOwner.id, firstOwner.accessToken);
    TestOAuthConnectionState firstOwnerConnection = getConnection(firstOwner.id);
    assertThat(firstOwnerConnection.getState().getAccessToken(), is(firstOwner.accessToken));

    // tests authorization
    simulateCallback(secondOwner.id, secondOwner.accessToken);
    TestOAuthConnectionState secondOwnerConnection = getConnection(secondOwner.id);
    assertThat(firstOwnerConnection.getState().getAccessToken(), is(firstOwner.accessToken));
    assertThat(secondOwnerConnection.getState().getAccessToken(), is(secondOwner.accessToken));

    // tests token refresh
    WireMock.reset();
    stubRefreshToken(firstOwner.refreshToken);
    flowRunner("refreshToken").withVariable(OWNER_ID_VARIABLE_NAME, firstOwner.id).run();
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
    assertThat(firstOwnerConnection.getState().getAccessToken(), is(firstOwner.refreshToken));
    assertThat(secondOwnerConnection.getState().getAccessToken(), is(secondOwner.accessToken));

    // tests token invalidation
    flowRunner("unauthorize").withVariable(OWNER_ID_VARIABLE_NAME, firstOwner.id).run();
    try {
      firstOwnerConnection.getState().getAccessToken();
      fail("Expected access token to be invalidated");
    } catch (Exception e) {
      assertThat(e.getClass().getName(), containsString("TokenInvalidatedException"));
    }

    assertThat(secondOwnerConnection.getState().getAccessToken(), is(secondOwner.accessToken));
  }

  private TestOAuthConnectionState getConnection(String resourceOwnerId) throws Exception {
    return ((TestOAuthConnection) flowRunner("getConnection")
        .withVariable(OWNER_ID_VARIABLE_NAME, resourceOwnerId)
        .run().getMessage().getPayload().getValue()).getState();
  }

  private static class ResourceOwnerData {

    public String id;
    public String accessToken;
    public String refreshToken;

    ResourceOwnerData(String id, String accessToken) {
      this.id = id;
      this.accessToken = accessToken;
      this.refreshToken = accessToken + "-refreshed";
    }
  }
}
