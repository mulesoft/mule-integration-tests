/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SimpleOpenTelemetryTracingTestCase extends AbstractOpenTelemetryTracingTestCase {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME = "tracing:with-correlation-id";
  private static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  private static final String EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME = "tracing:set-logging-variable";
  private static final String SIMPLE_FLOW = "simple-flow";
  private static final String CORRELATION_ID_KEY = "correlation.id";
  private static final String TEST_ARTIFACT_ID = "SimpleOpenTelemetryTracingTestCase#testSimpleFlow";
  private static final String FLOW_LOCATION = "simple-flow";
  private static final String SET_PAYLOAD_LOCATION = "simple-flow/processors/2";
  private static final String SET_VARIABLE_LOCATION = "simple-flow/processors/0/processors/0";
  private static final String SET_LOGGING_VARIABLE_LOCATION = "simple-flow/processors/1";
  private static final String TRACING_SET_CORRELATION_ID_LOCATION = "simple-flow/processors/0";
  private static final String TEST_VAR_NAME = "testVar";
  private static final String TRACE_VAR_VALUE = "Hello World!";
  private static final String CORRELATION_ID_CUSTOM_VALUE = "Fua";

  public SimpleOpenTelemetryTracingTestCase(String exporterType, String schema, int port, String path, boolean secure) {
    super(exporterType, schema, port, path, secure);
  }

  @Override
  protected String getConfigFile() {
    return "tracing/simple-flow.xml";
  }

  @Test
  public void testSimpleFlow() throws Exception {


    flowRunner(SIMPLE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = getSpans();
        return exportedSpans.size() == 5;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> exportedSpans = getSpans();

    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    String artifactId = TEST_ARTIFACT_ID + "[type: " + exporterType + " - path: " + super.path + " - secure: " + secure + "]";
    Map<String, String> setPayloadAttributeMap = createAttributeMap(SET_PAYLOAD_LOCATION, artifactId);
    setPayloadAttributeMap.put(TEST_VAR_NAME, TRACE_VAR_VALUE);
    Map<String, String> setVariableAttributeMap = createAttributeMap(SET_VARIABLE_LOCATION, artifactId);
    setVariableAttributeMap.put(CORRELATION_ID_KEY, CORRELATION_ID_CUSTOM_VALUE);

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(TRACING_SET_CORRELATION_ID_LOCATION, artifactId))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .beginChildren()
        .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
        .addAttributesToAssertValue(setVariableAttributeMap)
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .endChildren()
        .child(EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME)
        .addAttributesToAssertValue(createAttributeMap(SET_LOGGING_VARIABLE_LOCATION, artifactId))
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
        .addAttributesToAssertValue(setPayloadAttributeMap)
        .addAttributesToAssertExistence(attributesToAssertExistence)
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
    exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(artifactId)));
  }
}
