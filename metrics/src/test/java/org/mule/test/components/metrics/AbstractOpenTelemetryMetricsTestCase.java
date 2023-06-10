/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.metrics;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.metrics.exporter.api.MeterExporterProperties.METRIC_EXPORTER_ENABLED_PROPERTY;
import static org.mule.runtime.metrics.exporter.api.MeterExporterProperties.METRIC_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;

import org.mule.test.components.metrics.export.ExportedMeter;
import org.mule.test.components.metrics.export.OpenTelemetryMetricsTestUtils;

import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest.parseFrom;
import static org.testcontainers.Testcontainers.exposeHostPorts;
import static org.testcontainers.containers.BindMode.READ_ONLY;

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

  protected GenericContainer<?> collector;
  @ClassRule
  public static final TestGrpcServerRule server = new TestGrpcServerRule();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(MULE_ENABLE_STATISTICS, "true");
    setProperty(METRIC_EXPORTER_ENABLED_PROPERTY, "true");
    withContextClassLoader(GenericContainer.class.getClassLoader(), () -> {
      exposeHostPorts(server.httpPort());
      // Configuring the collector test-container
      collector =
          new GenericContainer<>(COLLECTOR_IMAGE)
              .withImagePullPolicy(PullPolicy.alwaysPull())
              .withEnv(
                       "OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + server.httpPort())
              .withClasspathResourceMapping(
                                            "otel.yaml", "/otel.yaml", READ_ONLY)
              .withCommand("--config", "/otel.yaml")
              .withExposedPorts(
                                COLLECTOR_OTLP_GRPC_PORT,
                                COLLECTOR_HEALTH_CHECK_PORT)
              .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

      collector.start();
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
      setProperty("mule.open.telelemetry.metric.exporter",
                  "http://" + collector.getHost() + ":" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT) + "/");
    });
    setProperty(METRIC_EXPORTER_ENDPOINT, "http://localhost:" + collector.getMappedPort(COLLECTOR_OTLP_GRPC_PORT));
  }

  @After
  public void after() {
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED);
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
