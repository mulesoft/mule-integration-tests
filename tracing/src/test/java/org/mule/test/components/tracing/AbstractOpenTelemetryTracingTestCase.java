/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;

import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest.parseFrom;

import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.verifyResourceAndScopeGrouping;
import static org.testcontainers.Testcontainers.exposeHostPorts;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.utility.MountableFile.forHostPath;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.SelfSignedCertificateRule;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractOpenTelemetryTracingTestCase extends
    MuleArtifactFunctionalTestCase implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final DockerImageName COLLECTOR_IMAGE =
      DockerImageName.parse("ghcr.io/open-telemetry/opentelemetry-java/otel-collector");

  private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
  private static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;
  private static final Integer COLLECTOR_OTLP_GRPC_MTLS_PORT = 5317;
  private static final Integer COLLECTOR_OTLP_HTTP_MTLS_PORT = 5318;
  private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;

  protected final String exporterType;
  private final String schema;
  private final int port;
  private final String path;
  private final boolean secure;

  protected GenericContainer<?> collector;

  @ClassRule
  public static SelfSignedCertificateRule serverTls = new SelfSignedCertificateRule();

  @ClassRule
  public static SelfSignedCertificateRule clientTls = new SelfSignedCertificateRule();

  @ClassRule
  public static final TestGrpcServerRule server = new TestGrpcServerRule();

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"GRPC", "http://", COLLECTOR_OTLP_GRPC_PORT, "", false},
        {"HTTP", "http://", COLLECTOR_OTLP_HTTP_PORT, "/v1/traces", false},
        {"GRPC", "https://", COLLECTOR_OTLP_GRPC_MTLS_PORT, "", true},
        {"HTTP", "https://", COLLECTOR_OTLP_HTTP_MTLS_PORT, "/v1/traces", true}
    });
  }

  public AbstractOpenTelemetryTracingTestCase(String exporterType, String schema, int port, String path, boolean secure) {
    this.exporterType = exporterType;
    this.schema = schema;
    this.port = port;
    this.path = path;
    this.secure = secure;
  }

  @Before
  public void before() {
    withContextClassLoader(GenericContainer.class.getClassLoader(), () -> {
      exposeHostPorts(server.httpPort());
      // Configuring the collector test-container
      collector =
          new GenericContainer<>(COLLECTOR_IMAGE)
              .withImagePullPolicy(PullPolicy.alwaysPull())
              .withCopyFileToContainer(
                                       forHostPath(serverTls.certificateFile().toPath(), 365),
                                       "/server.cert")
              .withCopyFileToContainer(
                                       forHostPath(serverTls.privateKeyFile().toPath(), 365), "/server.key")
              .withCopyFileToContainer(
                                       forHostPath(clientTls.certificateFile().toPath(), 365),
                                       "/client.cert")
              .withEnv("MTLS_CLIENT_CERTIFICATE", "/client.cert")
              .withEnv("MTLS_SERVER_CERTIFICATE", "/server.cert")
              .withEnv("MTLS_SERVER_KEY", "/server.key")
              .withEnv(
                       "OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + server.httpPort())
              .withClasspathResourceMapping(
                                            "otel.yaml", "/otel.yaml", READ_ONLY)
              .withCommand("--config", "/otel.yaml")
              .withExposedPorts(
                                COLLECTOR_OTLP_GRPC_PORT,
                                COLLECTOR_OTLP_HTTP_PORT,
                                COLLECTOR_OTLP_GRPC_MTLS_PORT,
                                COLLECTOR_OTLP_HTTP_MTLS_PORT,
                                COLLECTOR_HEALTH_CHECK_PORT)
              .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));

      collector.start();
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, exporterType);
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                  schema + collector.getHost() + ":" + collector.getMappedPort(port) + path);
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, Boolean.toString(secure));
      if (secure) {
        setProperty(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION, clientTls.privateKeyFile().toPath().toString());
        setProperty(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION, clientTls.certificateFile().toPath().toString());
        setProperty(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION, serverTls.certificateFile().toPath().toString());
      }
    });
  }

  @After
  public void after() {
    server.reset();
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION);
    clearProperty(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION);
  }

  protected List<CapturedExportedSpan> getSpans() {
    return server.getSpans();
  }

  private static final class TestGrpcServerRule extends ServerRule {

    public static final String PATH_PATTERN = "/opentelemetry.proto.collector.trace.v1.TraceService/Export";

    private final List<CapturedExportedSpan> capturedExportedSpans = new ArrayList<>();

    @Override
    protected void configure(ServerBuilder sb) {
      sb.service(PATH_PATTERN,
                 new AbstractUnaryGrpcService() {

                   @Override
                   protected @NotNull CompletionStage<byte[]> handleMessage(
                                                                            @NotNull ServiceRequestContext ctx,
                                                                            byte @NotNull [] message) {
                     try {
                       verifyResourceAndScopeGrouping(parseFrom(message));
                       capturedExportedSpans
                           .addAll(OpenTelemetryProtobufSpanUtils.getSpans(parseFrom(message)));
                     } catch (InvalidProtocolBufferException e) {
                       throw new UncheckedIOException(e);
                     }
                     return completedFuture(ExportTraceServiceResponse.getDefaultInstance().toByteArray());
                   }
                 });
      sb.http(0);
    }

    public void reset() {
      capturedExportedSpans.clear();
    }

    public List<CapturedExportedSpan> getSpans() {
      return capturedExportedSpans;
    }
  }

}
