/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl.client.ocsp;

import static org.mule.tls.fips.DefaultTestConfiguration.isFipsTesting;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class HttpRequesterOcspRevocationTestCase extends AbstractHttpOcspRevocationTestCase {

  @BeforeClass
  public static void before() {
    assumeFalse("W-16968647: Check that this is not in fips where the standard revocation check does not work. Another of the documented options should be used",
                isFipsTesting());
  }

  public HttpRequesterOcspRevocationTestCase(String configFile, String certAlias) {
    super(configFile, REVOKED_OCSP_LIST, certAlias);
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"http-requester-ocsp-revocation-config.xml", null},
        {"http-requester-ocsp-revocation-custom-provider-config.xml", "server"},
        {"http-requester-ocsp-revocation-custom-provider-config.xml", "thirdParty"}
    });
  }

  @Test
  public void testServerCertifiedAndRevoked() {
    var exception = assertThrows(Exception.class, this::runRevocationTestFlow);
    verifyRevocationException(exception);
  }
}
