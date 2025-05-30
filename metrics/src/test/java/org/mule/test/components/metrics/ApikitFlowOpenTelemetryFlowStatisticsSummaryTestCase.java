/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    return 1;
  }

  @Override
  long getExpectedDeclaredApikitFlows() {
    return 2;
  }

  @Override
  long getExpectedDeclaredTriggerFlows() {
    return 1;
  }

  @Override
  long getExpectedActivePrivateFlows() {
    return 1;
  }

  @Override
  long getExpectedActiveApikitFlows() {
    return 2;
  }

  @Override
  long getExpectedActiveTriggerFlows() {
    return 1;
  }

  @Override
  protected long getExpectedDeclaredPrivateFlowsV2() {
    return 1;
  }

  @Override
  protected long getExpectedDeclaredApikitFlowsV2() {
    return 2;
  }

  @Override
  protected long getExpectedActivePrivateFlowsV2() {
    return 1;
  }

  @Override
  protected long getExpectedActiveApikitFlowsV2() {
    return 2;
  }
}
