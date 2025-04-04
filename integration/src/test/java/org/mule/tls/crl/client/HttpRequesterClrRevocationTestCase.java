/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl.client;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class HttpRequesterClrRevocationTestCase extends AbstractHttpRequesterClrTestCase {

  public HttpRequesterClrRevocationTestCase(String configFile) {
    super(configFile, REVOKED_CRL_FILE_PATH, ENTITY_CERTIFIED_REVOCATION_SUB_PATH);
  }

  @Test
  public void testServerCertifiedAndRevoked() {
    var exception = assertThrows(Exception.class, this::runRevocationTestFlow);
    verifyRevocationException(exception);
  }
}
