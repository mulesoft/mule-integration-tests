/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package extension.org.mule.soap.it;

import static java.util.Collections.singletonList;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProviderConfigurationException;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.util.List;

public class TestServiceProvider implements SoapServiceProvider {

  private static final String ERROR_PORT = "ERROR_PORT";

  @Parameter
  private String url;

  @Parameter
  private String service;

  @Parameter
  private String port;

  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("1").withWsdlUrl(url).withService(service).withPort(port).build());
  }

  @Override
  public void validateConfiguration() throws SoapServiceProviderConfigurationException {
    if (port.equals(ERROR_PORT)) {
      throw new SoapServiceProviderConfigurationException("Error");
    }
  }

  public String getPort() {
    return port;
  }
}
