/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tck;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "weatherReport")
@XmlType(name = "WeatherReportType")
public class WeatherReportType {

  public String zipCode;
  public String report;
}
