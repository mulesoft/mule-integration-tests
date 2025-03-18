/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class HttpRequesterClrRevocationOutdatedCrlTestCase extends AbstractHttpRequesterClrTestCase {


  public HttpRequesterClrRevocationOutdatedCrlTestCase(String configFile) {
    super(configFile, OUTDATED_CRL_FILE_PATH, ENTITY_CERTIFIED_OUTDATED_CRL_SUB_PATH);
  }

  @Test
  public void testServerCertifiedAndOutdatedCrl() {
    var exception = assertThrows(Exception.class, this::runRevocationTestFlow);
    verifyUndeterminedRevocationException(exception);
  }
}
