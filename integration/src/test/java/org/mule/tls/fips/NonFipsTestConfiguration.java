/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.fips;

/**
 * A concrete implementation of {@link TestConfiguration} used for testing in environments that do not require FIPS (Federal
 * Information Processing Standards) compliance. This class provides configurations for non-FIPS-compliant tests.
 */
public class NonFipsTestConfiguration implements TestConfiguration {

  private static final String NON_FIPS_KEY_STORE_PK12_TYPE = "pkcs12";
  private static final String NON_FIPS_TRUST_STORE_JCEKS_TYPE = "jceks";
  private static final String NON_FIPS_CERTIFICATE_AUTHORITY_ENTITY = "tls/crl/certificate-authority-entity.p12";
  private static final String NON_FIPS_TRUST_FILE_FOR_CRL = "tls/crl/trustFile.jceks";

  @Override
  public String getKeyStorePKS12Type() {
    return NON_FIPS_KEY_STORE_PK12_TYPE;
  }

  @Override
  public String getTrustStoreJCEKSType() {
    return NON_FIPS_TRUST_STORE_JCEKS_TYPE;
  }

  @Override
  public String getCertificateAuthorityEntity() {
    return NON_FIPS_CERTIFICATE_AUTHORITY_ENTITY;
  }

  @Override
  public String getTrustFileForCrl() {
    return NON_FIPS_TRUST_FILE_FOR_CRL;
  }
}
