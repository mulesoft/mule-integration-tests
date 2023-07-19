/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.metrics;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;

import org.mule.tck.junit4.rule.DynamicPort;

import io.qameta.allure.Feature;
import org.junit.Rule;

@Feature(PROFILING)
public class ApikitFlowOpenTelemetryFlowStatisticsSummaryTestCase extends AbstractOpenTelemetryFlowStatisticsSummaryTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  private static final String RESOURCE_NAME = "ApikitFlowOpenTelemetryFlowStatisticsSummaryTestCase#test";

  @Override
  protected String getConfigFile() {
    return "metrics/apikit-flow-statistics-summary.xml";
  }

  @Override
  String getResourceName() {
    return RESOURCE_NAME;
  }

  @Override
  long getExpectedDeclaredPrivateFlows() {
    return 0;
  }

  @Override
  long getExpectedDeclaredApikitFlows() {
    return 1;
  }

  @Override
  long getExpectedDeclaredTriggerFlows() {
    return 1;
  }

  @Override
  long getExpectedActivePrivateFlows() {
    return 0;
  }

  @Override
  long getExpectedActiveApikitFlows() {
    return 1;
  }

  @Override
  long getExpectedActiveTriggerFlows() {
    return 1;
  }
}
