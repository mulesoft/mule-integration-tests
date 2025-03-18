/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.fips;

/**
 * Interface representing a configuration for testing. Provides methods to retrieve information about store types, SSL
 * certificates, key stores, and store passwords used during testing.
 */
public interface TestConfiguration {

  /**
   * Retrieves the key store type in PKCS#12 format.
   *
   * @return a {@code String} representing the PKCS#12 key store type.
   */
  String getKeyStorePKS12Type();

  /**
   * Retrieves the trust store type in JCEKS format.
   *
   * @return a {@code String} representing the JCEKS trust store type.
   */
  String getTrustStoreJCEKSType();

  /**
   * Resolves the password according to the environment.
   *
   * @param defaultPassword the default password
   * @return the resolvedPassword.
   */
  default String resolveStorePassword(String defaultPassword) {
    return defaultPassword;
  }

  /**
   * Retrieves the certificate authority entity.
   *
   * @return a {@code String} representing the certificate authority entity.
   */
  String getCertificateAuthorityEntity();

  /**
   * Retrieves the trust file used for Certificate Revocation List (CRL) validation.
   *
   * @return a {@code String} representing the trust file for CRL validation.
   */
  String getTrustFileForCrl();

}

