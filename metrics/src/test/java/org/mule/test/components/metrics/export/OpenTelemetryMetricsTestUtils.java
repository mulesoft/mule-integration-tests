/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.metrics.export;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;

/**
 * Utils for metrics testing.
 */
public class OpenTelemetryMetricsTestUtils {


  public static final String SERVICE_NAME_KEY = "service.name";

  /**
   * Returns the exported metrics from the protobuf representation.
   *
   * @param request the protobuf representation from the sdk.
   *
   * @return the list of retrieved meters.
   */
  public static List<ExportedMeter> getMetrics(ExportMetricsServiceRequest request) {
    List<ExportedMeter> exportedMeters = new ArrayList<>();

    for (ResourceMetrics resourceMetrics : request.getResourceMetricsList()) {
      // Adding the resource name.
      List<KeyValue> attributeKeyValues = resourceMetrics.getResource().getAttributesList();

      for (InstrumentationLibraryMetrics instrumentationLibraryMetrics : resourceMetrics.getInstrumentationLibraryMetricsList()) {
        for (Metric metrics : instrumentationLibraryMetrics.getMetricsList()) {
          ExportedMeter exportedMeter = new ExportedMeter();
          addResourceName(exportedMeter, attributeKeyValues);
          exportedMeter.setDescription(metrics.getDescription());
          exportedMeter.setInstrumentName(instrumentationLibraryMetrics.getInstrumentationLibrary().getName());
          exportedMeter.setName(metrics.getName());
          exportedMeter.setValue(metrics.getSum().getDataPoints(0).getAsInt());
          exportedMeters.add(exportedMeter);
        }
      }
    }

    return exportedMeters;
  }

  private static void addResourceName(ExportedMeter exportedMeter, List<KeyValue> attributeKeyValues) {
    for (KeyValue attributeKeyValue : attributeKeyValues) {
      if (attributeKeyValue.getKey().equals(SERVICE_NAME_KEY)) {
        exportedMeter.setResourceName(attributeKeyValue.getValue().getStringValue());
      }
    }
  }
}
