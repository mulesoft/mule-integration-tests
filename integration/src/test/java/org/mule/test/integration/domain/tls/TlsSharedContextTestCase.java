/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.tls;

import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Collections;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

import jakarta.inject.Inject;

@Ignore("MULE-10633")
@Issue("MULE-10633")
public class TlsSharedContextTestCase extends DomainFunctionalTestCase {

  public class RegistryHolder {

    @Inject
    public void setRegistry(Registry registry) {
      TlsSharedContextTestCase.this.domainRegistry = registry;
    }

  }

  private static final String DATA = "data";
  private static final String FIRST_APP = "firstApp";
  private static final String SECOND_APP = "secondApp";

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public DynamicPort port3 = new DynamicPort("port3");

  private Registry domainRegistry;

  @Override
  protected String getDomainConfig() {
    return "domain/tls/tls-domain-config.xml";
  }

  @Override
  protected Map<String, Object> getDomainStartUpRegistryObjects() {
    return Collections.singletonMap("RegistryHolder", new RegistryHolder());
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {new ApplicationConfig(FIRST_APP, new String[] {"domain/tls/tls-first-app-config.xml"}),
        new ApplicationConfig(SECOND_APP, new String[] {"domain/tls/tls-second-app-config.xml"})};
  }

  @Test
  public void sharedRequesterUsingSharedTlsContextToLocalListener() throws Exception {
    testFlowForApp("helloWorldClientFlow", FIRST_APP, "hello world");
  }

  @Test
  public void localRequesterToSharedListenerUsingSharedTlsContext() throws Exception {
    testFlowForApp("helloMuleClientFlow", SECOND_APP, "hello mule");
  }

  @Test
  public void muleClientUsingSharedTlsContextToListenerUsingSharedTlsContext() throws Exception {
    TlsContextFactory tlsContextFactory = domainRegistry.<TlsContextFactory>lookupByName("sharedTlsContext2").get();

    HttpClient httpClient = new TestHttpClient.Builder().tlsContextFactory(tlsContextFactory).build();
    httpClient.start();

    HttpRequest request = HttpRequest.builder().uri(format("https://localhost:%s/helloAll", port3.getValue())).method(GET)
        .entity(new ByteArrayHttpEntity(DATA.getBytes())).build();
    final HttpResponse response = httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);

    httpClient.stop();

    assertThat(IOUtils.toString(response.getEntity().getContent()), is("hello all"));
  }

  private void testFlowForApp(String flowName, String appName, String expected) throws Exception {
    CoreEvent response = new FlowRunner(getInfrastructureForApp(appName).getRegistry(), flowName).withPayload(DATA).run();

    final org.mule.runtime.core.api.MuleContext appMuleContext = getMuleContextForApp(appName);
    Message transformedMessage = appMuleContext.getTransformationService()
        .transform(response.getMessage(), DataType.builder()
            .type(String.class)
            .charset(getDefaultEncoding(appMuleContext.getConfiguration()))
            .build());

    assertThat((String) transformedMessage.getPayload().getValue(), is(expected));
  }
}
