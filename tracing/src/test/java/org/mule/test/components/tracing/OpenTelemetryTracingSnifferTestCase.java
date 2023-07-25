/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import org.junit.After;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;


public abstract class OpenTelemetryTracingSnifferTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER, "true");
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "true");
    super.doSetUpBeforeMuleContextCreation();
  }

  @After
  public void clearProperties() {
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED);
    clearProperty(USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER);
  }
}
