/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.test.components.tracing.OpenTelemetryProtobufSpanUtils.verifyResourceAndScopeGrouping;

import static java.lang.Boolean.TRUE;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static com.linecorp.armeria.common.HttpResponse.from;
import static com.linecorp.armeria.common.HttpStatus.OK;
import static io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest.parseFrom;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.protocol.AbstractUnaryGrpcService;
import com.linecorp.armeria.testing.junit4.server.SelfSignedCertificateRule;
import com.linecorp.armeria.testing.junit4.server.ServerRule;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractFullOpenTelemetryTracingTestCase extends
    OpenTelemetryTracingTestCase implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  public static final String HTTP_TRACES_PATH = "/v1/traces";
  protected final String exporterType;
  protected final String path;
  protected final boolean secure;

  @ClassRule
  public static SelfSignedCertificateRule serverTls = new SelfSignedCertificateRule();

  @ClassRule
  public static SelfSignedCertificateRule clientTls = new SelfSignedCertificateRule();

  @Rule
  public final TestGrpcServerRule server = new TestGrpcServerRule();

  @Parameterized.Parameters(name = "type: {0} - path: {1} - secure: {2}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"GRPC", "", false},
        {"HTTP", HTTP_TRACES_PATH, false},
        {"GRPC", "", true},
        {"HTTP", HTTP_TRACES_PATH, true}
    });
  }

  public AbstractFullOpenTelemetryTracingTestCase(String exporterType, String path, boolean secure) {
    this.exporterType = exporterType;
    this.path = path;
    this.secure = secure;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() {
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, TRUE.toString());
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_TYPE, exporterType);
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT,
                "http://localhost:" + server.httpPort() + path);
    setProperty(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, Boolean.toString(secure));
    if (secure) {
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION, clientTls.privateKeyFile().toPath().toString());
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION, clientTls.certificateFile().toPath().toString());
      setProperty(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION, serverTls.certificateFile().toPath().toString());
    }
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

      sb.service(HTTP_TRACES_PATH, new AbstractHttpService() {

        @Override
        protected HttpResponse doPost(ServiceRequestContext ctx, HttpRequest req) {
          return HttpResponse.from(req.aggregate().handle((aReq, cause) -> {
            CompletableFuture<HttpResponse> responseFuture = new CompletableFuture<>();
            HttpResponse res = from(responseFuture);

            try {
              capturedExportedSpans.addAll(OpenTelemetryProtobufSpanUtils.getSpans(parseFrom(aReq.content().array())));
            } catch (InvalidProtocolBufferException e) {
              throw new UncheckedIOException(e);
            }
            responseFuture.complete(HttpResponse.of(OK));
            return res;
          }));
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
