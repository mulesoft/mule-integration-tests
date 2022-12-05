package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.util.StringUtils.toHexString;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.tracer.api.sniffer.CapturedEventData;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AttributeKeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;

public class OpenTelemetryProtobufSpanUtils {

  private OpenTelemetryProtobufSpanUtils() {}

  public static class OpenTelemetryProtobufSpanFilter {

    private String locationPrefix = "";
    private final List<Span> spans = new ArrayList<>();

    public OpenTelemetryProtobufSpanFilter(ExportTraceServiceRequest exportTraceServiceRequest) {
      exportTraceServiceRequest.getResourceSpansList().forEach(resourceSpans -> resourceSpans.getInstrumentationLibrarySpansList()
          .forEach(instrumentationLibrarySpans -> spans.addAll(instrumentationLibrarySpans.getSpansList())));
    }

    public OpenTelemetryProtobufSpanFilter withLocationPrefix(String locationPrefix) {
      this.locationPrefix = locationPrefix;
      return this;
    }

    public List<? extends CapturedExportedSpan> filter() {
      List<SpanDataWrapper> totalSpans = spans.stream().map(SpanDataWrapper::new).collect(toList());
      if (!locationPrefix.isEmpty()) {
        return totalSpans.stream().filter(span -> span.getLocation().startsWith(locationPrefix)).collect(toList());
      } else {
        return totalSpans;
      }
    }
  }

  private static final class SpanDataWrapper implements CapturedExportedSpan {

    private final Span openTelemetryProtobufSpan;

    public SpanDataWrapper(Span openTelemetryProtobufSpan) {
      this.openTelemetryProtobufSpan = openTelemetryProtobufSpan;
    }

    @Override
    public String getName() {
      return openTelemetryProtobufSpan.getName();
    }

    @Override
    public String getParentSpanId() {
      return toHexString(openTelemetryProtobufSpan.getParentSpanId().toByteArray());
    }

    @Override
    public String getSpanId() {
      return toHexString(openTelemetryProtobufSpan.getSpanId().toByteArray());
    }

    @Override
    public Map<String, String> getAttributes() {
      return openTelemetryProtobufSpan.getAttributesList().stream()
          .collect(toMap(AttributeKeyValue::getKey, attributeKeyValue -> attributeKeyValue.getUnknownFields().toByteString()
              .toString(StandardCharsets.UTF_8).trim()));
    }

    @Override
    public List<CapturedEventData> getEvents() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getServiceName() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean hasErrorStatus() {
      return !openTelemetryProtobufSpan.getStatus().getCode().equals(Status.StatusCode.Ok);
    }

    public String getLocation() {
      return getAttributes().get("location");
    }

    @Override
    public String toString() {
      return String.format("a span with name: [%s], ID: [%s] and parent Span ID: [%s]", getName(), getSpanId(),
                           getParentSpanId());
    }
  }
}
