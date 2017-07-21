/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package extension.org.mule.soap.it;

import static java.util.Collections.singletonList;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.extension.api.soap.WebServiceDefinition.builder;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.util.List;

public class TestServiceProvider implements SoapServiceProvider {

  public static final String ERROR_MSG = "ERROR";

  @Parameter
  private String url;

  @Parameter
  private String service;

  @Parameter
  private String port;

  @Parameter
  @Optional(defaultValue = "true")
  private boolean valid;

  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    return singletonList(builder().withId("1").withWsdlUrl(url).withService(service).withPort(port).build());
  }

  @Override
  public ConnectionValidationResult validate() {
    return valid ? success() : failure(ERROR_MSG, new RuntimeException(ERROR_MSG));
  }
}
