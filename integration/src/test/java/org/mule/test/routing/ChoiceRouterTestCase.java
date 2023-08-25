/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.routing;

import static java.lang.Runtime.getRuntime;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ChoiceStory.CHOICE;
import static org.mule.test.routing.ThreadCaptor.getCapturedThreads;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(CHOICE)
public class ChoiceRouterTestCase extends AbstractIntegrationTestCase {

  private static final int LOAD = getRuntime().availableProcessors() * 10;

  @Override
  protected String getConfigFile() {
    return "routers/choice-router-config.xml";
  }

  @Test
  public void noDefaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("flow").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD + "afterRouteMpCounter"));
  }

  @Test
  public void defaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("otherwise").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD + "otherwiseCounter" + "afterCounter"));
  }

  @Test
  public void multipleMatchingRoutes() throws Exception {
    Message result = flowRunner("multiple").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD + "first" + "after"));
  }

  @Test
  public void errorsWithinRouteArePropagated() throws Exception {
    assertMultipleErrors("error-handler", "handled");
  }

  @Test
  public void errorsWithinRouteExpressionArePropagated() throws Exception {
    assertMultipleErrors("expression", "handled");
  }

  @Test
  public void errorsWithinTryRouteArePropagated() throws Exception {
    assertMultipleErrors("try-error-handler", "handled after try");
  }

  @Test
  public void errorsWithinTryRouteExpressionArePropagated() throws Exception {
    assertMultipleErrors("try-expression", "handled after try");
  }

  private void assertMultipleErrors(String expression, String expected) throws Exception {
    for (int i = 0; i < LOAD; i++) {
      Message message = flowRunner(expression).withPayload(TEST_PAYLOAD).run().getMessage();
      assertThat(message, hasPayload(equalTo(expected)));
    }
  }

  @Test
  public void txWithNonBlockingRoute() throws Exception {
    Message result = flowRunner("txNonBlocking").withPayload("nonBlocking").run().getMessage();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  public void txWithCpuIntensiveRoute() throws Exception {
    Message result = flowRunner("txCpuIntensive").withPayload("cpuIntensive").run().getMessage();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  public void txWithBlockingRoute() throws Exception {
    Message result = flowRunner("txBlocking").withPayload("blocking").run().getMessage();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  public void txWithOtherwise() throws Exception {
    Message result = flowRunner("txOtherwise").withPayload("ooo").run().getMessage();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  public void txWithNoOtherwise() throws Exception {
    Message result = flowRunner("txNoOtherwise").withPayload("ooo").run().getMessage();
    assertThat(getCapturedThreads(), hasSize(1));
  }

  @Test
  @Issue("MULE-18803")
  @Description("Verify that using a non-blocking processor in the default route of a choice is not flaky."
      + "This was flaky because of a race condition between the processing of the defaut route and the completion of that flux for that route when the choice was iniside a Mono component.")
  public void nonBlockingProcessorInDefaultRoute() throws Exception {
    for (int i = 0; i < 5000; ++i) {
      final FlowRunner flowRunner = flowRunner("nonBlockingProcessorInDefaultRoute").withPayload("ooo");
      flowRunner.run();
    }
  }

}
