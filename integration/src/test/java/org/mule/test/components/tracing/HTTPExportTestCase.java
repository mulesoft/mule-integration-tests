/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_TRACING_EXPORT;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tests.api.TestQueueManager;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_TRACING_EXPORT)
public class HTTPExportTestCase extends AbstractIntegrationTestCase {

  private static final String SIMPLE_FLOW = "simpleFlow";

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "tracing/tracing-http-export.xml";
  }

  @Rule
  public SystemProperty exportEnabled =
          new SystemProperty("mule.openetelemetry.export.enabled", "true");
  @Rule
  public SystemProperty exportBatchSize =
          new SystemProperty("mule.opentelemetry.export.batch.size", "1");
  @Rule
  public SystemProperty exportProtocol =
          new SystemProperty("mule.opentelemetry.export.protocol", "HTTP");
  @Rule
  public SystemProperty exportEndpoint = new SystemProperty("mule.opentelemetry.endpoint", "http://localhost:8080/v1/traces");

  @Test
  public void testSimpleFlow() throws Exception {
    flowRunner(SIMPLE_FLOW).run();
    CoreEvent firstEvent = queueManager.read("exportedSpans", RECEIVE_TIMEOUT, MILLISECONDS);
    assertThat(queueManager.read("exportedSpans", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
  }

  public static class OpenTelemetryProtobufSpansParser implements Processor {
    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        List<Span> spans = new ArrayList<>();
        ExportTraceServiceRequest traceServiceRequest = ExportTraceServiceRequest.parseFrom((InputStream) ((CursorProvider) event.getMessage().getPayload().getValue()).openCursor());
        traceServiceRequest.getResourceSpansList().forEach(resourceSpans -> resourceSpans.getInstrumentationLibrarySpansList().forEach(
                instrumentationLibrarySpans -> spans.addAll(instrumentationLibrarySpans.getSpansList())
        ));
        return CoreEvent.builder(event).message(Message.of(spans)).build();
      } catch (Throwable t) {
        throw new MuleRuntimeException(createStaticMessage("Error trying to get the exported Spans"), t);
      }
    }
  }
}
