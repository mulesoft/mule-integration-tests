/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.security;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.commons.httpclient.auth.AuthScope.ANY;
import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-13203: This test uses app's spring-security lib which conflicts with the ine included in spring-module")
public class AuthenticationAgainstMultipleProvidersTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort httpPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort httpPort3 = new DynamicPort("port3");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/security/mule-multiple-providers-config-flow.xml";
  }

  @Test
  public void testProvider1() throws Exception {
    HttpClient httpClient = new HttpClient();
    Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
    httpClient.getState().setCredentials(ANY, credentials);
    httpClient.getParams().setAuthenticationPreemptive(true);

    PostMethod postMethod = new PostMethod("http://localhost:" + httpPort1.getNumber());
    postMethod.setDoAuthentication(true);
    postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

    assertEquals(SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("hello", postMethod.getResponseBodyAsString());

    credentials = new UsernamePasswordCredentials("asdf", "asdf");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

    credentials = new UsernamePasswordCredentials("admin2", "admin2");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));
  }

  @Test
  public void testProvider2() throws Exception {
    HttpClient httpClient = new HttpClient();
    Credentials credentials = new UsernamePasswordCredentials("admin2", "admin2");
    httpClient.getState().setCredentials(ANY, credentials);
    httpClient.getParams().setAuthenticationPreemptive(true);

    PostMethod postMethod = new PostMethod("http://localhost:" + httpPort2.getNumber());
    postMethod.setDoAuthentication(true);
    postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

    assertEquals(SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("hello", postMethod.getResponseBodyAsString());

    credentials = new UsernamePasswordCredentials("asdf", "asdf");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

    credentials = new UsernamePasswordCredentials("admin", "admin");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));
  }

  @Test
  public void testMultipleProviders() throws Exception {
    HttpClient httpClient = new HttpClient();
    Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
    httpClient.getState().setCredentials(ANY, credentials);
    httpClient.getParams().setAuthenticationPreemptive(true);

    PostMethod postMethod = new PostMethod("http://localhost:" + httpPort3.getNumber());
    postMethod.setDoAuthentication(true);
    postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

    assertEquals(SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("hello", postMethod.getResponseBodyAsString());

    credentials = new UsernamePasswordCredentials("asdf", "asdf");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

    credentials = new UsernamePasswordCredentials("admin2", "admin2");
    httpClient.getState().setCredentials(ANY, credentials);
    assertEquals(SC_OK, httpClient.executeMethod(postMethod));
    assertEquals("hello", postMethod.getResponseBodyAsString());
  }

}
