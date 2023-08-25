/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tls;

import static java.lang.String.format;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;

import io.qameta.allure.Issue;
import org.apache.activemq.broker.SslBrokerService;
import org.junit.Rule;
import org.junit.Test;

public class TlsInsecureTrustStoreJmsTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_RESPONSE = "Message received was: test";
  private static final String KEYSTORE_PATH = "test-classes/chain-cert-keystore.jks";
  private static final String KEYSTORE_PASS = "changeit";
  private static final String KEYSTORE_KEY_PASS = "changeit";

  private final SslBrokerService brokerService = new SslBrokerService();

  @Rule
  public DynamicPort portSsl = new DynamicPort("portSsl");

  @Override
  protected String getConfigFile() {
    return "tls/tls-insecure-truststore-jms-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    brokerService.addSslConnector(format("ssl://localhost:%d", portSsl.getNumber()), getKeyManagerFactory().getKeyManagers(),
                                  null, null);
    brokerService.start();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    brokerService.stop();
    super.doTearDownAfterMuleContextDispose();
  }

  @Test
  @Issue("W-12049036")
  public void whenUsingInsecureTrustStoreAndServerCertificateIsUntrustedThenConnectionCanBeEstablished() throws Exception {
    CoreEvent response = flowRunner("flow-publish-consume-insecure").run();
    assertThat(response.getMessage().getPayload().getValue(), equalTo(EXPECTED_RESPONSE));
  }

  private KeyManagerFactory getKeyManagerFactory()
      throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
    KeyStore ks = KeyStore.getInstance("JKS");
    try (InputStream ksIs = new FileInputStream(KEYSTORE_PATH)) {
      ks.load(ksIs, KEYSTORE_PASS.toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, KEYSTORE_KEY_PASS.toCharArray());

    return kmf;
  }
}
