package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class UntilSuccessfulSuccessTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:until-successful:route";
  public static final String EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME = "mule:until-successful";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String UNTIL_SUCCESSFUL_FLOW = "until-successful-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/until-successful-success.xml";
  }

  @Test
  public void testUntilSuccessfulFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(UNTIL_SUCCESSFUL_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan untilSuccessfulSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> setPayloadSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME))
              .collect(Collectors.toList());
      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(0).getSpanId())).findFirst()
              .orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(5));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(untilSuccessfulSpan, notNullValue());
      assertThat(untilSuccessfulSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(1));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(untilSuccessfulSpan.getSpanId())));
      assertThat(setPayloadSpanList, hasSize(1));
      assertThat(setPayloadSpan, notNullValue());
      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(muleRouteSpanList.get(0).getSpanId()));
    } finally {
      spanCapturer.dispose();
    }
  }
}
