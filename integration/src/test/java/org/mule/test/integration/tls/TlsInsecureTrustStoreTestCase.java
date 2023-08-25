/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tls;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

public class TlsInsecureTrustStoreTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_RESPONSE = "test";

  @Rule
  public DynamicPort portSsl = new DynamicPort("portSsl");

  @Override
  protected String getConfigFile() {
    return "tls/tls-insecure-truststore-config.xml";
  }

  @Test
  public void whenUsingInsecureTrustStoreAndBothCertificatesAreUntrustedThenConnectionCanBeEstablished() throws Exception {
    CoreEvent response = flowRunner("flow-insecure-request").run();
    assertThat(response.getMessage().getPayload().getValue(), equalTo(EXPECTED_RESPONSE));
  }

  @Test
  @Issue("W-10822938")
  public void whenUsingInsecureTrustStoreWithoutClientCertificateThenConnectionCanBeEstablished() throws Exception {
    CoreEvent response = flowRunner("flow-insecure-request-no-client-cert").run();
    assertThat(response.getMessage().getPayload().getValue(), equalTo(EXPECTED_RESPONSE));
  }
}
