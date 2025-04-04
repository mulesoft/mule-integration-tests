/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.tests.api.TestQueueManager;

import java.io.IOException;

import jakarta.inject.Inject;

import io.qameta.allure.Feature;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

@Feature(HTTP_EXTENSION)
public class HttpListenerAuthenticationTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  private static final String BASIC_REALM_MULE_REALM = "Basic realm=\"mule-realm\", charset=\"UTF-8\"";
  private static final String VALID_USER = "user";
  private static final String VALID_PASSWORD = "password";
  private static final String INVALID_PASSWORD = "invalidPassword";
  private static final String EXPECTED_PAYLOAD = "TestBasicAuthOk";
  CloseableHttpClient httpClient;
  CloseableHttpResponse httpResponse;

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/security/http-listener-authentication-config.xml";
  }

  @After
  public void tearDown() {
    closeQuietly(httpResponse);
    closeQuietly(httpClient);
  }

  @Test
  public void invalidBasicAuthentication() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, INVALID_PASSWORD);
    getHttpResponse(credsProvider);

    assertUnauthorised();
    assertThat(queueManager.read("basicAuthentication", RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), is(notNullValue()));
  }

  @Test
  public void validBasicAuthentication() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, VALID_PASSWORD);
    getHttpResponse(credsProvider);

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(EXPECTED_PAYLOAD));
  }

  @Test
  public void noProvider() throws Exception {
    CredentialsProvider credsProvider = getCredentialsProvider(VALID_USER, VALID_PASSWORD);
    getHttpResponse(credsProvider, "zaraza");

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(INTERNAL_SERVER_ERROR.getReasonPhrase()));
    assertThat(queueManager.read("basicAuthentication", RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), is(notNullValue()));
  }

  @Test
  public void extendedEncodedHeader() throws Exception {
    HttpPost httpPost = new HttpPost(format("http://localhost:%s/basic?provider=%s", listenPort.getNumber(), "memory-provider"));
    httpPost.addHeader(AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==PEPE");
    httpClient = HttpClients.createDefault();
    httpResponse = httpClient.execute(httpPost);
    assertResultForExtendedHeader();
  }

  protected void assertResultForExtendedHeader() {
    assertUnauthorised();
  }

  protected void assertUnauthorised() {
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(UNAUTHORIZED.getStatusCode()));
    Header authHeader = httpResponse.getFirstHeader(WWW_AUTHENTICATE);
    assertThat(authHeader, is(notNullValue()));
    assertThat(authHeader.getValue(), is(BASIC_REALM_MULE_REALM));
  }

  private void getHttpResponse(CredentialsProvider credsProvider) throws IOException {
    getHttpResponse(credsProvider, "memory-provider");
  }

  private void getHttpResponse(CredentialsProvider credsProvider, String provider) throws IOException {
    HttpPost httpPost = new HttpPost(format("http://localhost:%s/basic?provider=%s", listenPort.getNumber(), provider));
    httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
    httpResponse = httpClient.execute(httpPost);
  }

  private CredentialsProvider getCredentialsProvider(String user, String password) {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    return credsProvider;
  }

}
