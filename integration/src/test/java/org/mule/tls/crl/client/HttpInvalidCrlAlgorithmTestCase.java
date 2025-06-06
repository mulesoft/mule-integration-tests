/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tls.crl.client;

import static org.mule.tls.fips.DefaultTestConfiguration.isFipsTesting;

import static java.util.Arrays.asList;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class HttpInvalidCrlAlgorithmTestCase extends AbstractConfigurationFailuresTestCase {

  @BeforeClass
  public static void before() {
    assumeFalse("W-16968647: Check that this is not in fips where the standard revocation check does not work. Another of the documented options should be used",
                isFipsTesting());
  }

  public static final String INVALID_CRL_ALGORITHM_SunX509 =
      "TLS Context: certificate revocation checking is only available for algorithm PKIX (current value is SunX509)";

  @Test
  public void testInvalidCrlAlgorithm() {
    var exception = assertThrows(Exception.class, () -> loadConfiguration("http-requester-tls-crl-invalid-algorithm-config.xml"));
    Throwable rootCause = getRootCause(exception);
    assertThat(rootCause, instanceOf(CreateException.class));
    assertThat(rootCause.getMessage(), is(INVALID_CRL_ALGORITHM_SunX509));
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel mule = MuleExtensionModelProvider.getExtensionModel();
    ExtensionModel tls = MuleExtensionModelProvider.getTlsExtensionModel();
    return asList(mule, tls);
  }
}
