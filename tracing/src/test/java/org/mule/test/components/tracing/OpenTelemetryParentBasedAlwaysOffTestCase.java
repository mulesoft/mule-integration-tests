/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetryParentBasedAlwaysOffTestCase extends OpenTelemetrySamplerTestCase {

  @Override
  String getSamplerName() {
    return "parentbased_always_off";
  }

  @Override
  String getSamplerArg() {
    return null;
  }
}
