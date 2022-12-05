/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.MULE_OPENTELEMETRY_ENDPOINT_SYSPROP;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.MULE_OPENTELEMETRY_EXPORT_BATCH_SIZE_SYSPROP;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.MULE_OPENTELEMETRY_PROTOCOL_SYSPROP;
import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.OPENTELEMETRY_EXPORT_ENABLED_SYSPROP;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_TRACING_EXPORT;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.tests.api.TestQueueManager;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_TRACING_EXPORT)
public class HTTPExportTestCase extends AbstractIntegrationTestCase {

  private static final String SIMPLE_FLOW = "simpleFlow";
  public static final String EXPORT_ENDPOINT = "http://localhost:8080/v1/traces";
  public static final String HTTP = "HTTP";
  public static final String ONE = "1";

  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_OPERATION_SPAN = "heisenberg:call-gus-fring";
  private static final String ERROR_TYPE = "HEISENBERG:CONNECTIVITY";
  private static final String EXPECTED_ON_ERROR_CONTINUE_SPAN = "mule:on-error-continue";

  @Inject
  private TestQueueManager queueManager;

  @Inject
  private ProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/tracing-http-export.xml";
  }

  @Rule
  public SystemProperty exportEnabled =
      new SystemProperty(OPENTELEMETRY_EXPORT_ENABLED_SYSPROP, TRUE.toString());
  @Rule
  public SystemProperty exportBatchSize =
      new SystemProperty(MULE_OPENTELEMETRY_EXPORT_BATCH_SIZE_SYSPROP, ONE);
  @Rule
  public SystemProperty exportProtocol =
      new SystemProperty(MULE_OPENTELEMETRY_PROTOCOL_SYSPROP, HTTP);
  @Rule
  public SystemProperty exportEndpoint = new SystemProperty(MULE_OPENTELEMETRY_ENDPOINT_SYSPROP, EXPORT_ENDPOINT);

  @Test
  public void testSimpleFlow() throws Exception {
    flowRunner(SIMPLE_FLOW).withProfilingService((PrivilegedProfilingService) profilingService).run();
    List<CapturedExportedSpan> collectedSpans = new ArrayList<>();
    do {
      collectedSpans.addAll((List<CapturedExportedSpan>) queueManager.read("exportedSpans", RECEIVE_TIMEOUT, MILLISECONDS)
          .getMessage().getPayload().getValue());
    } while (collectedSpans.size() != 3);
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(collectedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_OPERATION_SPAN).addExceptionData(ERROR_TYPE)
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren();
  }

  public static class OpenTelemetryProtobufSpansFilter implements Processor {

    public OpenTelemetryProtobufSpansFilter() {}

    @Override
    public CoreEvent process(CoreEvent coreEvent) throws MuleException {
      try {
        return filterSpans(coreEvent);
      } catch (Throwable t) {
        throw new MuleRuntimeException(createStaticMessage("Error trying to filter the collected Spans"), t);
      }
    }

    private CoreEvent filterSpans(CoreEvent event) throws IOException {
      ExportTraceServiceRequest exportTraceServiceRequest = ExportTraceServiceRequest
          .parseFrom((InputStream) ((CursorProvider<Cursor>) event.getMessage().getPayload().getValue()).openCursor());
      List<?> filteredSpans = new OpenTelemetryProtobufSpanUtils.OpenTelemetryProtobufSpanFilter(exportTraceServiceRequest)
          .withLocationPrefix(SIMPLE_FLOW).filter();
      return CoreEvent.builder(event).message(Message.of(filteredSpans)).build();
    }

  }

}
