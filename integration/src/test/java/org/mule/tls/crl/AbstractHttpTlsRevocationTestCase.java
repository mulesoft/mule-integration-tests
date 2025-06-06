/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tls.fips.DefaultTestConfiguration.isFipsTesting;

import static java.security.cert.CRLReason.KEY_COMPROMISE;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateRevokedException;

import org.junit.Rule;

public abstract class AbstractHttpTlsRevocationTestCase extends AbstractIntegrationTestCase {

  private static final String UNDETERMINED_REVOCATION_ERROR_MESSAGE = "Could not determine revocation status";

  /**
   * Each CRL has a distant nextUpdate date for avoiding their expiration, except the outdatedCrl that was created for testing
   * undetermined revocation scenarios.
   */
  protected static final String EMPTY_CRL_FILE_PATH = "tls/crl/emptyCrl";

  protected static final String REVOKED_CRL_FILE_PATH = "tls/crl/validCrl";

  protected static final String OUTDATED_CRL_FILE_PATH = "tls/crl/outdatedCrl";

  protected static final String NO_CR_LS_FOUND_FOR_ISSUER_BC_ERROR_MESSGE = "No CRLs found for issuer";

  protected static final String KEY_COMPROMISE_BC_ERROR_MESSAGE = "keyCompromise";

  /**
   * For avoiding flaky tests, it is necessary to use consistently the certified entities. Each certified entity (i.e. each
   * certificate ) has a hardcoded crl distribution with the format: http://localhost:8093/crl/{numberOfTheEntity}. Java SSL
   * Support caches the CRLs, so it only hits the crl server once per URI. Therefore, in test cases, we should always use:
   * {@value ENTITY_CERTIFIED_NO_REVOCATION_SUB_PATH } for no revocation scenarios where tls/crl/emptyCrl list should be returned,
   * {@value ENTITY_CERTIFIED_OUTDATED_CRL_SUB_PATH } for undetermined revocation scenarios where tls/crl/outdatedCrl list should
   * be returned and {@value ENTITY_CERTIFIED_REVOCATION_SUB_PATH } for revocation scenarios where tls/crl/validCrl list should be
   * returned.
   */
  protected static final String ENTITY_CERTIFIED_NO_REVOCATION_SUB_PATH = "entity1";

  protected static final String ENTITY_CERTIFIED_OUTDATED_CRL_SUB_PATH = getEntity2KeyStore();

  protected static final String ENTITY_CERTIFIED_REVOCATION_SUB_PATH = "entity3";

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public SystemProperty crlSystemProperty;

  @Rule
  public SystemProperty entityCertifiedSubPathSystemProperty;

  public String configFile;

  public AbstractHttpTlsRevocationTestCase(String configFile, String crlPath, String entityCertified) {
    this.configFile = configFile;
    crlSystemProperty = new SystemProperty("crlPath", crlPath);
    entityCertifiedSubPathSystemProperty = new SystemProperty("entityCertifiedSubPath", entityCertified);
  }

  public AbstractHttpTlsRevocationTestCase(String configFile, String entityCertified) {
    this.configFile = configFile;
    entityCertifiedSubPathSystemProperty = new SystemProperty("entityCertifiedSubPath", entityCertified);
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  protected CoreEvent runRevocationTestFlow() throws Exception {
    return flowRunner("testFlowRevoked").withPayload("data").keepStreamsOpen().run();
  }

  protected void verifyUndeterminedRevocationException(Throwable e) {
    Throwable rootException = getRootCause(e);
    if (isFipsTesting()) {
      // The exception of bouncy castle is not exported.
      // We assert the message.
      assertThat(rootException.getMessage(), containsString(NO_CR_LS_FOUND_FOR_ISSUER_BC_ERROR_MESSGE));
    } else {
      assertThat(rootException, is(instanceOf(CertPathValidatorException.class)));
      assertThat(rootException.getMessage(), is(UNDETERMINED_REVOCATION_ERROR_MESSAGE));
    }
  }

  protected void verifyRevocationException(Throwable e) {
    Throwable rootException = getRootCause(e);
    if (isFipsTesting()) {
      // The exception of bouncy castle is not exported.
      // We assert the message.
      assertThat(rootException.getMessage(), containsString(KEY_COMPROMISE_BC_ERROR_MESSAGE));
    } else {
      assertThat(rootException, is(instanceOf(CertificateRevokedException.class)));
      assertThat(((CertificateRevokedException) rootException).getRevocationReason(), is(KEY_COMPROMISE));
    }
  }

  protected void verifyNotRevokedEntity() throws Exception {
    CoreEvent result = runRevocationTestFlow();
    assertThat(result.getMessage(), hasPayload(equalTo("OK")));
  }

  private static String getEntity2KeyStore() {
    if (isFipsTesting()) {
      return "entity2-fips";
    }

    return "entity2";
  }
}
