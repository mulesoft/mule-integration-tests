/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package extension.org.mule.soap.it;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.util.List;
import java.util.Map;

@Alias("custom-headers")
public class TestServiceProviderWithCustomHeaders implements SoapServiceProvider {

  @Parameter
  private String url;

  @Parameter
  private String service;

  @Parameter
  private String port;

  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("2").withWsdlUrl(url).withService(service).withPort(port).build());
  }

  @Override
  public Map<String, String> getCustomHeaders(WebServiceDefinition definition, String operation) {
    return singletonMap("headerIn", "<con:headerIn xmlns:con=\"http://service.soap.service.mule.org/\">OP=" + operation + "</con:headerIn>");
  }
}
