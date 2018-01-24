/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.it.soap.connect;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.service.soap.SoapTestUtils.assertSimilarXml;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.extension.api.soap.SoapServiceProviderConfigurationException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class SoapExtensionTestConnectivityTestCase extends AbstractSimpleServiceFunctionalTestCase {

  @Inject
  @Named(CONNECTIVITY_TESTING_SERVICE_KEY)
  private ConnectivityTestingService service;

  @Override
  protected String getConfigFile() {
    return "test-connectivity.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Test
  public void success() {
    ConnectionValidationResult result = service.testConnection(Location.builder().globalName("success").build());
    assertThat(result.isValid(), is(true));
  }

  @Test
  public void failureOnServiceProvider() {
    ConnectionValidationResult result = service.testConnection(Location.builder().globalName("failureOnServiceProvider").build());
    assertThat(result.isValid(), is(false));
    assertThat(result.getException(), instanceOf(SoapServiceProviderConfigurationException.class));
    assertThat(result.getMessage(), is("Error"));
  }

  @Test
  public void failureOnDispatcherProvider() {
    ConnectionValidationResult result =
        service.testConnection(Location.builder().globalName("failureOnDispatcherProvider").build());
    assertThat(result.isValid(), is(false));
    assertThat(result.getMessage(), containsString("invalid requester name"));
  }

  @Test
  public void failureOnDispatcherProviderWithServiceProvider() {
    ConnectionValidationResult result =
        service.testConnection(Location.builder().globalName("failureOnDispatcherProviderUsingService").build());
    assertThat(result.isValid(), is(false));
    assertThat(result.getMessage(), containsString("invalid port name"));
  }
}
