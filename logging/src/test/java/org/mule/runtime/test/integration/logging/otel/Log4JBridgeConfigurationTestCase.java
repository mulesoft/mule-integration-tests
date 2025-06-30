/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging.otel;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SIMPLE_LOG;
import static org.mule.runtime.logging.otel.configuration.api.OpenTelemetryLoggingConfigurationProperties.MULE_OPEN_TELEMETRY_LOGGING_EXPORTER;
import static org.mule.runtime.logging.otel.configuration.api.OpenTelemetryLoggingConfigurationProperties.MULE_OPEN_TELEMETRY_LOGGING_EXPORTER_ENABLED;
import static org.mule.runtime.logging.otel.configuration.api.log4j.OpenTelemetryLog4JBridge.OPEN_TELEMETRY_APPENDER_NAME_SUFFIX;
import static org.mule.runtime.logging.otel.configuration.impl.OpenTelemetryLoggingFactory.getLogRecordSniffer;
import static org.mule.tck.probe.PollingProber.probe;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.logging.otel.configuration.api.testing.ExportedLogRecordSniffer;
import org.mule.runtime.logging.otel.configuration.api.testing.SniffedLogRecord;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.test.integration.logging.AbstractLogConfigurationTestCase;
import org.mule.runtime.test.integration.logging.util.UseMuleLog4jContextFactory;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class Log4JBridgeConfigurationTestCase extends AbstractLogConfigurationTestCase {

  public static final String APP_NAME = "otel-logging-application";

  private final ApplicationFileBuilder loggingAppFileBuilder =
      new ApplicationFileBuilder(APP_NAME).definedBy("log/otel/otel-logging.xml");

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Rule
  public SystemProperty enableOTELLogging = new SystemProperty(MULE_OPEN_TELEMETRY_LOGGING_EXPORTER_ENABLED, "true");

  @Rule
  public SystemProperty enableOTELLoggingSniffing =
      new SystemProperty(MULE_OPEN_TELEMETRY_LOGGING_EXPORTER + ".use.sniffer", "true");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // here we're trying to test log separation so we need to
    // disable this default property of the fake mule server
    // in order to test that
    System.clearProperty(MULE_SIMPLE_LOG);
    muleServer.start();
  }

  @Test
  public void applicationWithOTELLLoggingEnabled() throws Exception {
    ExportedLogRecordSniffer exportedLogRecordSniffer = getLogRecordSniffer();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    assertThat(true, equalTo(loggerHasAppender(APP_NAME, getRootLoggerForApp(APP_NAME),
                                               APP_NAME + OPEN_TELEMETRY_APPENDER_NAME_SUFFIX)));
    probe(() -> exportedLogRecordSniffer.getSniffedLogRecords().size() == 1);
    SniffedLogRecord sniffedLogRecord = exportedLogRecordSniffer.getSniffedLogRecords().get(0);
    assertThat(sniffedLogRecord.getBody(), equalTo("Test"));
    assertThat(sniffedLogRecord.getSeverity(), equalTo("INFO"));
  }

}
