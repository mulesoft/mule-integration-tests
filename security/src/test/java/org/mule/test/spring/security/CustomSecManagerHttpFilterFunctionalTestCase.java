/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.spring.security;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class CustomSecManagerHttpFilterFunctionalTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/security/custom-security-manager-test.xml";
  }

  protected String getUrl() {
    return "http://localhost:" + dynamicPort.getValue() + "/authenticate";
  }

  @Test
  public void testAuthenticationFailureNoContext() throws Exception {
    HttpClient client = new HttpClient();
    client.getParams().setAuthenticationPreemptive(true);
    GetMethod get = new GetMethod(getUrl());

    get.setDoAuthentication(false);

    try {
      int status = client.executeMethod(get);
      assertThat(status, is(SC_UNAUTHORIZED));
      assertThat(get.getResponseBodyAsString(),
                 containsString("no security context on the session. Authentication denied on connector"));
    } finally {
      get.releaseConnection();
    }
  }

  @Test
  public void testAuthenticationFailureBadCredentials() throws Exception {
    doRequest(null, "localhost", "anonX", "anonX", getUrl(), false, SC_UNAUTHORIZED);
  }

  @Ignore // TODO Realm validataion seems to be completely ignored
  @Test
  public void testAuthenticationFailureBadRealm() throws Exception {
    doRequest("blah", "localhost", "anon", "anon", getUrl(), false, SC_UNAUTHORIZED);
  }

  @Test
  public void testAuthenticationAuthorised() throws Exception {
    doRequest(null, "localhost", "anon", "anon", getUrl(), false, SC_OK);
  }

  @Test
  public void testAuthenticationAuthorisedWithHandshake() throws Exception {
    doRequest(null, "localhost", "anon", "anon", getUrl(), true, SC_OK);
  }

  @Ignore // TODO Realm validataion seems to be completely ignored
  @Test
  public void testAuthenticationAuthorisedWithHandshakeAndBadRealm() throws Exception {
    doRequest("blah", "localhost", "anon", "anon", getUrl(), true, SC_UNAUTHORIZED);
  }

  @Test
  public void testAuthenticationAuthorisedWithHandshakeAndRealm() throws Exception {
    doRequest("mule-realm", "localhost", "ross", "ross", getUrl(), true, SC_OK);
  }

  private void doRequest(String realm, String host, String user, String pass, String url, boolean handshake, int result)
      throws Exception {
    HttpClient client = new HttpClient();
    client.getParams().setAuthenticationPreemptive(true);
    client.getState().setCredentials(new AuthScope(host, -1, realm), new UsernamePasswordCredentials(user, pass));
    GetMethod get = new GetMethod(url);
    get.setDoAuthentication(handshake);

    try {
      int status = client.executeMethod(get);
      if (status == SC_UNAUTHORIZED && handshake == true) {
        // doAuthentication = true means that if the request returns 401,
        // the HttpClient will resend the request with credentials
        status = client.executeMethod(get);
      }
      assertEquals(result, status);
    } finally {
      get.releaseConnection();
    }
  }
}

