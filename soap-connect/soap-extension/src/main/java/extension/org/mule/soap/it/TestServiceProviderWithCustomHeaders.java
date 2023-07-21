/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package extension.org.mule.soap.it;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProviderConfigurationException;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.util.List;
import java.util.Map;

@Alias("custom-headers")
public class TestServiceProviderWithCustomHeaders implements SoapServiceProvider, Initialisable, Disposable {

  public static Environment ENVIRONMENT;

  @Parameter
  private String url;

  @Parameter
  private String service;

  @Parameter
  private String port;

  @Parameter
  @Optional(defaultValue = "PROD")
  private Environment environment;

  @Override
  public void initialise() throws InitialisationException {
    ENVIRONMENT = environment;
  }

  @Override
  public void dispose() {
    ENVIRONMENT = null;
  }

  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("2").withWsdlUrl(url).withService(service).withPort(port).build());
  }

  @Override
  public Map<String, String> getCustomHeaders(WebServiceDefinition definition, String operation) {
    return singletonMap("headerIn", "<con:headerIn xmlns:con=\"http://service.soap.service.mule.org/\">OP=" + operation + "</con:headerIn>");
  }

  public Environment getEnvironment() {
    return environment;
  }
}
