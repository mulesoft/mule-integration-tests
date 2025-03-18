/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl;

import static org.mule.tls.fips.DefaultTestConfiguration.isFipsTesting;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import java.security.cert.CertPathValidatorException;

import org.junit.BeforeClass;
import org.junit.Test;

public class HttpPreferringCrlTestCase extends AbstractHttpTlsRevocationTestCase {

  public static final String EXPECTED_OCSP_ERROR_MESSAGE = "Certificate does not specify OCSP responder";

  public HttpPreferringCrlTestCase() {
    super("http-requester-tls-crl-standard-config.xml", REVOKED_CRL_FILE_PATH, ENTITY_CERTIFIED_REVOCATION_SUB_PATH);
  }

  @BeforeClass
  public static void before() {
    assumeFalse("W-16968647: Check that this is not in fips where the standard revocation check does not work. Another of the documented options should be used",
                isFipsTesting());
  }

  @Test
  public void testPreferCrlWithFallback() {
    verifyRevocationForFlow("testFlowPreferCrl");
  }

  @Test
  public void testPreferCrlNoFallback() {
    verifyRevocationForFlow("testFlowPreferCrlNoFallback");
  }

  @Test
  public void testNotPreferCrlWithFallback() {
    verifyRevocationForFlow("testFlowNotPreferCrl");
  }

  @Test
  public void testNotPreferCrlNoFallback() {
    var exception = assertThrows(Exception.class, () -> runFlow("testFlowNotPreferCrlNoFallback"));
    Throwable rootException = getRootCause(exception);
    assertThat(rootException, is(instanceOf(CertPathValidatorException.class)));
    assertThat(rootException.getMessage(), is(EXPECTED_OCSP_ERROR_MESSAGE));
  }

  private void verifyRevocationForFlow(String testFlowPreferCrl) {
    var exception = assertThrows(Exception.class, () -> runFlow(testFlowPreferCrl));
    verifyRevocationException(exception);
  }
}
