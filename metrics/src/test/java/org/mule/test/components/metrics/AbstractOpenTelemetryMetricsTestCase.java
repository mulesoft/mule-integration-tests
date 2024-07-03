/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.metrics;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.metrics.exporter.api.MeterExporterProperties.METRIC_EXPORTER_ENABLED_PROPERTY;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT;

import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest.parseFrom;

import org.mule.test.components.metrics.export.ExportedMeter;
import org.mule.test.components.metrics.export.OpenTelemetryMetricsTestUtils;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Rule;

/**
 * Generic metrics case for integration metrics case.
 */
public abstract class AbstractOpenTelemetryMetricsTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryMetricsTestRunnerConfigAnnotation {

  private static final String EXPORTER_CONF_FILE = "conf/meter-exporter.conf";

  @Rule
  public final TestGrpcServerRule server = new TestGrpcServerRule();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(MULE_ENABLE_STATISTICS, TRUE.toString());
    setProperty(METRIC_EXPORTER_ENABLED_PROPERTY, TRUE.toString());

    String configurationFilePath = getResourceAsUrl(EXPORTER_CONF_FILE, getClass()).toURI().getPath();
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH, configurationFilePath);
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED, TRUE.toString());
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT,
                "http://localhost:" + server.httpPort());
  }

  @After
  public void after() {
    clearProperty(MULE_ENABLE_STATISTICS);
    clearProperty(METRIC_EXPORTER_ENABLED_PROPERTY);
    clearProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH);
    clearProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED);
    clearProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT);
    server.reset();
  }

  /**
   * A Test Grpc Server Rule that captures the metrics. Till reset, it will obtain only the first metrics exported.
   */
  protected static final class TestGrpcServerRule extends ServerRule {

    public static final String PATH_PATTERN = "/opentelemetry.proto.collector.metrics.v1.MetricsService/Export";
    private List<ExportedMeter> metrics;

    @Override
    protected void configure(ServerBuilder sb) {
      sb.service(PATH_PATTERN,
                 new AbstractUnaryGrpcService() {

                   @Override
                   protected @NotNull CompletionStage<byte[]> handleMessage(
                                                                            @NotNull ServiceRequestContext ctx,
                                                                            byte @NotNull [] message) {
                     try {
                       if (metrics == null) {
                         metrics = OpenTelemetryMetricsTestUtils.getMetrics(parseFrom(message));
                       }
                     } catch (InvalidProtocolBufferException e) {
                       throw new UncheckedIOException(e);
                     }
                     return completedFuture(ExportTraceServiceResponse.getDefaultInstance().toByteArray());
                   }
                 });
      sb.http(0);
    }

    public List<ExportedMeter> getMetrics() {
      return metrics;
    }

    public void reset() {
      metrics = null;
    }
  }

}
