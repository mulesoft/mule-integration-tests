/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;

import static java.util.Collections.synchronizedList;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

//public class OAuthHandlerIntegrationHandler extends MuleArtifactFunctionalTestCase {
public class OAuthHandlerIntegrationTestCase extends MuleArtifactFunctionalTestCase {

  private static List<Object> payloads;
  private static final int HTTP_STATUS_OK = 200;
  public static final int RESPONSE_TIMEOUT = 5000;

  @ClassRule
  public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /*
   * @Rule public DynamicPort port = new DynamicPort("port");
   */
  public String port = "8081";

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  public static class Captor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      Authentication token =
          (Authentication) (event.getSecurityContext()
              .getAuthentication());
      assertThat(token, is(notNullValue()));
      assertThat(token.getPrincipal(), is(notNullValue()));
      payloads.add(event.getMessage().getPayload().getValue());
      return event;
    }
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    payloads = synchronizedList(new ArrayList<>());
  }

  @Override
  protected String getConfigFile() {
    return "oauthHandlerWithObjectStore.xml";
  }

  @Test
  @Issue("W-11493901")
  @Description("verifiy object store is created if not found")
  public void execute() throws Exception {

    // HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port.getNumber() + "/createClientz").method(GET)
    HttpRequest request = HttpRequest.builder().uri("http://localhost:" + port + "/createClient").method(GET)
        .addHeader("client_id", "abc")
        .addHeader("client_secret", "abcdefg")
        .addHeader("client_name", "highkl")
        .build();
    HttpResponse response = httpClient.send(request, HttpRequestOptions.builder().responseTimeout(RESPONSE_TIMEOUT).build());
    assertThat(response.getStatusCode(), is(HTTP_STATUS_OK));
    String content = IOUtils.toString(response.getEntity().getContent());
    System.out.println("Content" + content);
    // awaitJobTermination();
  }

  private static final String getBasicAuthenticationHeader(String username, String password) {
    String valueToEncode = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
  }
}
