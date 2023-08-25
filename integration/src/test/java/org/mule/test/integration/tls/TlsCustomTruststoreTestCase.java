/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tls;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class TlsCustomTruststoreTestCase extends AbstractIntegrationTestCase {

  private static final String RESPONSE = "test";

  @Rule
  public DynamicPort portSsl = new DynamicPort("portSsl");

  @Rule
  public SystemProperty customTrustStoreEnabled =
      new SystemProperty("javax.net.ssl.trustStore", "test-classes/chain-cert-truststore.jks");

  @Override
  protected String getConfigFile() {
    return "tls/tls-clustom-truststore-config.xml";
  }

  @Test
  public void usingCustomTlsTrustManager() throws Exception {
    CoreEvent response = flowRunner("flow-custom").run();
    assertThat(response.getMessage().getPayload().getValue(), equalTo(RESPONSE));
  }

  @Test
  public void usingDefaultTlsTrustManager() throws Exception {
    CoreEvent response = flowRunner("flow-default").run();
    assertThat(response.getMessage().getPayload().getValue(), equalTo(RESPONSE));
  }
}
