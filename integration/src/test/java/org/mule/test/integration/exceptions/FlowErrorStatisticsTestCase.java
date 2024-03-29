/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.util.MuleSystemProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class FlowErrorStatisticsTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty muleEnableStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"flow-error-statistics-config.xml", "defaultErrorHandlerFlow"},
        {"flow-error-statistics-default-config.xml", "defaultConfigErrorHandlerFlow"},
        {"flow-error-statistics-config.xml", "referencedErrorHandlerFlow"},
        {"flow-error-statistics-config.xml", "innerErrorHandlerFlow"}
    });
  }

  private String configFile;
  private String flowName;

  boolean statisticsEnabledOriginal;

  public FlowErrorStatisticsTestCase(String configFile, String flowName) {
    this.configFile = configFile;
    this.flowName = flowName;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/" + configFile;
  }

  @Test
  public void executionErrors() throws Exception {
    flowRunner(flowName).runExpectingException();

    FlowConstructStatistics flowConstructStatistics = locator.find(Location.builder().globalName(flowName).build())
        .map(f -> (FlowConstruct) f).map(f -> f.getStatistics()).get();

    assertThat(flowConstructStatistics.getExecutionErrors(), is(1L));
  }
}
