/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.fips;

import static java.lang.Boolean.getBoolean;

public class DefaultTestConfiguration implements TestConfiguration {

  public static DefaultTestConfiguration getDefaultEnvironmentConfiguration() {
    return new DefaultTestConfiguration();
  }

  public static final String NAME_TESTING_SYS_PROP = "mule.fips.testing";

  public static boolean isFipsTesting() {
    return getBoolean(NAME_TESTING_SYS_PROP);
  }

  private final TestConfiguration delegate;

  private DefaultTestConfiguration() {
    this.delegate = resolveTestEnvironmentConfiguration();
  }

  private TestConfiguration resolveTestEnvironmentConfiguration() {
    if (isFipsTesting()) {
      return new FipsTestConfiguration();
    }
    return new NonFipsTestConfiguration();
  }

  @Override
  public String getKeyStorePKS12Type() {
    return delegate.getKeyStorePKS12Type();
  }

  @Override
  public String getTrustStoreJCEKSType() {
    return delegate.getTrustStoreJCEKSType();
  }

  @Override
  public String getCertificateAuthorityEntity() {
    return delegate.getCertificateAuthorityEntity();
  }

  @Override
  public String getTrustFileForCrl() {
    return delegate.getTrustFileForCrl();
  }
}
