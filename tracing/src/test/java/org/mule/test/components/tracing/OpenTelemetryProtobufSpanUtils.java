/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.core.api.util.StringUtils.toHexString;

import static java.util.Collections.emptyMap;

import static io.opentelemetry.api.trace.propagation.internal.W3CTraceContextEncoding.decodeTraceState;

import static java.util.stream.Collectors.toMap;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AttributeKeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;

/**
 * Utils for the open telemetry protobuf.
 *
 * @since 4.5.0
 */
public class OpenTelemetryProtobufSpanUtils {

  private static final String NO_PARENT_SPAN = "0000000000000000";

  public static List<? extends CapturedExportedSpan> getSpans(ExportTraceServiceRequest exportTraceServiceRequest) {
    return exportTraceServiceRequest.getResourceSpansList().stream()
        .flatMap(resourceSpans -> processResourceSpans(resourceSpans).stream())
        .collect(Collectors.toList());
  }

  private static List<SpanDataWrapper> processResourceSpans(ResourceSpans resourceSpans) {
    List<Span> spans = new ArrayList<>();
    // TODO: verify a better way to parse the attributes.
    String serviceName =
        resourceSpans.getResource().getAttributes(0).getUnknownFields().toByteString().toStringUtf8().substring(4);

    resourceSpans.getInstrumentationLibrarySpansList()
        .forEach(instrumentationLibrarySpans -> spans.addAll(instrumentationLibrarySpans.getSpansList()));

    return spans.stream().map(span -> new SpanDataWrapper(serviceName, span)).collect(Collectors.toList());
  }

  /**
   * Verify that there is only one resource / instrumentation scope
   *
   * @link <a href="https://opentelemetry.io/docs/reference/specification/resource/sdk/">Resource Definition</a>
   * @link <a href="https://opentelemetry.io/docs/reference/specification/glossary/#instrumentation-scope">Instrumentation Scope
   *       Definition</a>
   * @link <a href=
   *       "https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/collector/trace/v1/trace_service.proto">Export
   *       Request Protobuf</a>
   *
   * @param request the export request for traces.
   */
  public static void verifyResourceAndScopeGrouping(ExportTraceServiceRequest request) {

    assertThat("The number of expected resources for the export request is not 1. In the mule runtime there is only one resource per app",
               request.getResourceSpansCount(), equalTo(1));
    assertThat("The number of expected instrumentation scopes for the export request is not 1. In the mule runtime there is only one scope, which corresponds to the instrumentation code/library for open telemetry export module.",
               request.getResourceSpans(0).getInstrumentationLibrarySpansCount(), equalTo(1));
  }

  private static final class SpanDataWrapper implements CapturedExportedSpan {

    private final Span openTelemetryProtobufSpan;
    private final String serviceName;

    public SpanDataWrapper(String serviceName, Span openTelemetryProtobufSpan) {
      this.serviceName = serviceName;
      this.openTelemetryProtobufSpan = openTelemetryProtobufSpan;
    }

    @Override
    public String getName() {
      return openTelemetryProtobufSpan.getName();
    }

    @Override
    public String getParentSpanId() {
      String parentSpanId = toHexString(openTelemetryProtobufSpan.getParentSpanId().toByteArray());

      if (isEmpty(parentSpanId)) {
        parentSpanId = NO_PARENT_SPAN;
      }

      return parentSpanId;
    }

    @Override
    public String getSpanId() {
      return toHexString(openTelemetryProtobufSpan.getSpanId().toByteArray());
    }

    @Override
    public String getTraceId() {
      return toHexString(openTelemetryProtobufSpan.getTraceId().toByteArray());
    }

    @Override
    public Map<String, String> getAttributes() {
      // TODO: verify a better way to parse the attributes in optel protobuf.
      return openTelemetryProtobufSpan.getAttributesList().stream()
          .collect(toMap(AttributeKeyValue::getKey,
                         attributeKeyValue -> attributeKeyValue.getUnknownFields().toByteString().toStringUtf8().substring(4)));
    }

    @Override
    public List<CapturedEventData> getEvents() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getServiceName() {
      return serviceName;
    }

    @Override
    public String getSpanKindName() {
      return openTelemetryProtobufSpan.getKind().name();
    }

    @Override
    public boolean hasErrorStatus() {
      return !openTelemetryProtobufSpan.getStatus().getCode().equals(Status.StatusCode.Ok);
    }

    @Override
    public String getStatusAsString() {
      return openTelemetryProtobufSpan.getStatus().toString();
    }

    public String getLocation() {
      return getAttributes().get("location");
    }

    @Override
    public String toString() {
      return String.format("a span with name: [%s], ID: [%s] and parent Span ID: [%s]", getName(), getSpanId(),
                           getParentSpanId());
    }

    @Override
    public long getStartEpochSpanNanos() {
      return openTelemetryProtobufSpan.getStartTimeUnixNano();
    }

    @Override
    public long getEndSpanEpochNanos() {
      return openTelemetryProtobufSpan.getEndTimeUnixNano();
    }

    @Override
    public Map<String, String> getTraceState() {
      if (!openTelemetryProtobufSpan.getTraceState().isEmpty()) {
        return decodeTraceState(openTelemetryProtobufSpan.getTraceState()).asMap();
      }
      return emptyMap();
    }

  }

}
