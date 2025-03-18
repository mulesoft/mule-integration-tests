/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.fips;

/**
 * A concrete implementation of {@link TestConfiguration} that is used for testing in environments with FIPS (Federal Information
 * Processing Standards) compliance. This class provides configurations specific to FIPS-compliant tests.
 */
public class FipsTestConfiguration implements TestConfiguration {

  public static final String FIPS_TEST_STORE_PASSWORD = "mulepassword";
  public static final String FIPS_TEST_STORE_TYPE = "bcfks";
  private static final String FIPS_CERTIFICATE_AUTHORITY_ENTITY = "tls/crl/certificate-authority-entity.bcfks";
  private static final String FIPS_TRUST_FILE_FOR_CRL = "tls/crl/trustFile-fips.bcfks";

  @Override
  public String getKeyStorePKS12Type() {
    return FIPS_TEST_STORE_TYPE;
  }

  @Override
  public String getTrustStoreJCEKSType() {
    return FIPS_TEST_STORE_TYPE;
  }

  @Override
  public String resolveStorePassword(String defaultPassword) {
    return FIPS_TEST_STORE_PASSWORD;
  }

  @Override
  public String getCertificateAuthorityEntity() {
    return FIPS_CERTIFICATE_AUTHORITY_ENTITY;
  }

  @Override
  public String getTrustFileForCrl() {
    return FIPS_TRUST_FILE_FOR_CRL;
  }
}
