/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.metrics;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;

import io.qameta.allure.Feature;

@Feature(PROFILING)
public class PrivateFlowOpenTelemetryFlowStatisticsSummaryTestCase extends AbstractOpenTelemetryFlowStatisticsSummaryTestCase {

  private static final String RESOURCE_NAME = "PrivateFlowOpenTelemetryFlowStatisticsSummaryTestCase#test";

  @Override
  protected String getConfigFile() {
    return "metrics/private-flow-statistics-summary.xml";
  }

  @Override
  String getResourceName() {
    return RESOURCE_NAME;
  }

  @Override
  long getExpectedDeclaredPrivateFlows() {
    return 1;
  }

  @Override
  long getExpectedDeclaredApikitFlows() {
    return 0;
  }

  @Override
  long getExpectedDeclaredTriggerFlows() {
    return 0;
  }

  @Override
  long getExpectedActivePrivateFlows() {
    return 1;
  }

  @Override
  long getExpectedActiveApikitFlows() {
    return 0;
  }

  @Override
  long getExpectedActiveTriggerFlows() {
    return 0;
  }
}
