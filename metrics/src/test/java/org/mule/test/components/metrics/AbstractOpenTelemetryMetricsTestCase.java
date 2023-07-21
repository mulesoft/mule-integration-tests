/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.metrics;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
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
import static org.testcontainers.Testcontainers.exposeHostPorts;
import static org.testcontainers.containers.BindMode.READ_ONLY;

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
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * Generic metrics case for integration metrics case.
 */
public abstract class AbstractOpenTelemetryMetricsTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryMetricsTestRunnerConfigAnnotation {

  private static final DockerImageName COLLECTOR_IMAGE =
      DockerImageName.parse("ghcr.io/open-telemetry/opentelemetry-java/otel-collector");

  private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
  private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;

  private static final String EXPORTER_CONF_FILE = "conf/meter-exporter.conf";

  protected GenericContainer<?> collector;

  @ClassRule
  public static final TestGrpcServerRule server = new TestGrpcServerRule();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    withContextClassLoader(GenericContainer.class.getClassLoader(), () -> {
      exposeHostPorts(server.httpPort());
      // Configuring the collector test-container
      collector =
          new GenericContainer<>(COLLECTOR_IMAGE)
              .withImagePullPolicy(PullPolicy.alwaysPull())
              .withEnv("OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + server.httpPort())
              .withClasspathResourceMapping("otel.yaml", "/otel.yaml", READ_ONLY)
              .withCommand("--config", "/otel.yaml")
              .withExposedPorts(COLLECTOR_OTLP_GRPC_PORT, COLLECTOR_HEALTH_CHECK_PORT)
              .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

      collector.start();
    });

    setProperty(MULE_ENABLE_STATISTICS, TRUE.toString());
    setProperty(METRIC_EXPORTER_ENABLED_PROPERTY, TRUE.toString());

    String configurationFilePath = getResourceAsUrl(EXPORTER_CONF_FILE, getClass()).toURI().getPath();
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_CONFIGURATION_FILE_PATH, configurationFilePath);
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED, TRUE.toString());
    setProperty(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENDPOINT,
                "http://localhost:" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT));
  }

  @After
  public void after() {
    collector.stop();
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
