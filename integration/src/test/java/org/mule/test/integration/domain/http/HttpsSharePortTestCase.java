/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.domain.http;

import io.qameta.allure.Issue;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Before;
import org.junit.Ignore;

@Ignore("MULE-10633")
@Issue("MULE-10633")
public class HttpsSharePortTestCase extends HttpSharePortTestCase {

  private TlsContextFactory tlsContextFactory;

  @Before
  public void setup() throws CreateException {
    // Configure trust store in the client with the certificate of the server.
    tlsContextFactory = TlsContextFactory.builder()
        .trustStorePath("ssltest-cacerts.jks")
        .trustStorePassword("changeit")
        .build();
  }

  @Override
  protected String getDomainConfig() {
    return "domain/http/https-shared-listener-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http/http-hello-world-app.xml"}),
        new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http/http-hello-mule-app.xml"})};
  }

  @Override
  protected SystemProperty getEndpointSchemeSystemProperty() {
    return new SystemProperty("scheme", "https");
  }

  @Override
  protected TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }
}
