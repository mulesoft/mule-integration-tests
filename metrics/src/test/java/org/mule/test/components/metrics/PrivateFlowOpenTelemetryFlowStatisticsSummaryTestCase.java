/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
