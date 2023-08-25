/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tck;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(serviceName = "WeatherForecaster", portName = "WeatherForecasterPort")
public class WeatherForecaster {

  @WebMethod(operationName = "GetWeatherByZipCode")
  public String getByZipCode(String zipCode) {
    return zipCode + ": cloudy with chances of meatballs.";
  }
}
