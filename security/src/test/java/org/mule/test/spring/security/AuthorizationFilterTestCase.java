/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationFilterTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/security/http-module-filter-test.xml";
  }

  @Test
  public void testNotAuthenticated() throws Exception {
    doRequest("localhost", getUrl(), UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void testAuthenticatedButNotAuthorized() throws Exception {
    String body = doRequest(null, "localhost", "anon", "anon", getUrl(), false, OK.getStatusCode());
    assertThat(body, equalTo("unauthorized"));
  }

  @Test
  public void testAuthorized() throws Exception {
    String body = doRequest(null, "localhost", "ross", "ross", getUrl(), false, OK.getStatusCode());
    assertThat(body, equalTo("authorized"));
  }

  @Test
  public void testAuthorizedInAnotherFlow() throws Exception {
    String body = doRequest(null, "localhost", "ross", "ross", getUrl(), false, OK.getStatusCode());
    assertThat(body, equalTo("authorized"));
  }

  protected String getUrl() {
    return "http://localhost:" + port1.getNumber() + "/authorize";
  }

  private void doRequest(String host, String url, int result) throws Exception {
    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(url);
    try {
      int status = client.executeMethod(get);
      assertThat(result, is(status));
      assertThat(get.getResponseHeader(WWW_AUTHENTICATE), not(nullValue()));
      assertThat(get.getResponseHeader(WWW_AUTHENTICATE).getValue(), containsString("mule-realm"));
    } finally {
      get.releaseConnection();
    }
  }

  private String doRequest(String realm, String host, String user, String pass, String url, boolean handshake, int result)
      throws Exception {
    HttpClient client = new HttpClient();
    client.getParams().setAuthenticationPreemptive(true);
    client.getState().setCredentials(new AuthScope(host, -1, realm), new UsernamePasswordCredentials(user, pass));
    GetMethod get = new GetMethod(url);
    String body;
    get.setDoAuthentication(handshake);

    try {
      int status = client.executeMethod(get);
      if (status == UNAUTHORIZED.getStatusCode() && handshake == true) {
        // doAuthentication = true means that if the request returns 401,
        // the HttpClient will resend the request with credentials
        status = client.executeMethod(get);
        if (status == UNAUTHORIZED.getStatusCode() && handshake == true) {
          // doAuthentication = true means that if the request returns 401,
          // the HttpClient will resend the request with credentials
          status = client.executeMethod(get);
        }
      }
      assertEquals(result, status);
    } finally {
      body = get.getResponseBodyAsString();
      get.releaseConnection();
    }
    return body;
  }

}
