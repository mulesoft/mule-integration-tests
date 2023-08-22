/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER;

import static java.lang.Boolean.TRUE;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

/**
 * Abstract class for CE OTEL tests.
 */
public abstract class OpenTelemetryTracingSnifferTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty enableTracing = new SystemProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());

  @Rule
  public SystemProperty enableSniffing = new SystemProperty(USE_MULE_OPEN_TELEMETRY_EXPORTER_SNIFFER, TRUE.toString());
}
