/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth;

import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.http.api.HttpService;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;
import org.mule.service.http.TestHttpClient;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OAuthHandlerObjectStoreTestCase extends BaseOAuthExtensionTestCase {

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    wireMock.stubFor(post(urlPathMatching("/" + TOKEN_PATH)).willReturn(aResponse()
        .withStatus(OK.getStatusCode())
        .withBody(accessTokenContent())
        .withHeader(CONTENT_TYPE, "application/json")));
  }

  protected String[] getConfigFiles() {
    return new String[] {"oauthHandlerWithObjectStoreFlows.xml"};
  }

  @Test
  @Issue("W-11493901")
  @Description("verifiy object store is always created")
  public void verifyObjectStoreCreationDuringOAuth() throws Exception {

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();
    ObjectStore os = getObjectStore(CUSTOM_STORE_NAME);
    assertThat(os, notNullValue());
    assertThat(connection.getConnectionDetails(), notNullValue());
  }
}
