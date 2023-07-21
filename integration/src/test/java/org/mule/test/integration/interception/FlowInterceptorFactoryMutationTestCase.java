/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.interception;

import static java.lang.Math.random;
import static java.util.Arrays.asList;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.FLOW_INTERCEPTION_STORY;

import org.mule.runtime.api.interception.FlowInterceptor;
import org.mule.runtime.api.interception.FlowInterceptorFactory;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(FLOW_INTERCEPTION_STORY)
@RunnerDelegateTo(Parameterized.class)
public class FlowInterceptorFactoryMutationTestCase extends AbstractIntegrationTestCase {

  private final boolean mutateEventBefore;
  private final boolean mutateEventAfter;

  public FlowInterceptorFactoryMutationTestCase(boolean mutateEventBefore, boolean mutateEventAfter) {
    this.mutateEventBefore = mutateEventBefore;
    this.mutateEventAfter = mutateEventAfter;
  }

  @Parameters(name = "mutateEventBefore: {0}, mutateEventAfter: {1}")
  public static Collection<Boolean[]> data() {
    return asList(new Boolean[][] {
        {true, true},
        {true, false},
        {false, true},
        {false, false}
    });
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/flow-interceptor-factory-mutation.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_MutateEventInterceptorFactory", new MutateEventInterceptorFactory(mutateEventBefore, mutateEventAfter));

    return objects;
  }

  @Test
  public void mutated() {
    String targetFlow;
    if (!mutateEventBefore && !mutateEventAfter) {
      targetFlow = "mutatedNone";
    } else if (mutateEventBefore && !mutateEventAfter) {
      targetFlow = "mutatedBefore";
    } else if (!mutateEventBefore && mutateEventAfter) {
      targetFlow = "mutatedAfter";
    } else {
      targetFlow = "mutatedBeforeAfter";
    }

    flowRunner("mutated").withVariable("targetFlow", targetFlow);
  }

  public class MutateEventInterceptorFactory implements FlowInterceptorFactory {

    private final boolean mutateEventBefore;
    private final boolean mutateEventAfter;

    public MutateEventInterceptorFactory(boolean mutateEventBefore, boolean mutateEventAfter) {
      this.mutateEventBefore = mutateEventBefore;
      this.mutateEventAfter = mutateEventAfter;
    }

    @Override
    public boolean intercept(String flowName) {
      return !"mutated".equals(flowName);
    }

    @Override
    public FlowInterceptor get() {
      return new MutateEventInterceptor(mutateEventBefore, mutateEventAfter);
    }

  }

  public class MutateEventInterceptor implements FlowInterceptor {

    private final boolean mutateEventBefore;
    private final boolean mutateEventAfter;

    public MutateEventInterceptor(boolean mutateEventBefore, boolean mutateEventAfter) {
      this.mutateEventBefore = mutateEventBefore;
      this.mutateEventAfter = mutateEventAfter;
    }

    @Override
    public void before(String flowName, InterceptionEvent event) {
      if (mutateEventBefore) {
        event.addVariable("mutatedBefore", random());
      }
    }

    @Override
    public void after(String flowName, InterceptionEvent event, Optional<Throwable> thrown) {
      if (mutateEventAfter) {
        event.addVariable("mutatedAfter", random());
      }
    }
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

}
