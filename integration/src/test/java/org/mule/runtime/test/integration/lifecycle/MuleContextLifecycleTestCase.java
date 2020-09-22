/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.lifecycle;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LifecyclePhaseFailureStory.LIFECYCLE_PHASE_FAILURE_STORY;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tests.api.pojos.LifecycleObject;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.integration.AbstractConfigurationFailuresTestCase;
import org.mule.tests.api.TestComponentsExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(LIFECYCLE_PHASE_FAILURE_STORY)
public class MuleContextLifecycleTestCase extends AbstractConfigurationFailuresTestCase {

  private static final String EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION = "Expected a ContextStartException exception";

  @Test
  @Ignore("MULE-15693")
  public void failOnStartInvokesStopInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("lifecycle/component-failing-during-startup-config.xml",
                                  failOnStartLifecycleBean -> new PollingProber().check(new JUnitLambdaProbe(() -> {
                                    LifecycleObject lifecycleBean = failOnStartLifecycleBean.getOtherLifecycleObject();
                                    assertThat(failOnStartLifecycleBean.getCalledPhases(), hasSize(3));
                                    assertThat(failOnStartLifecycleBean.getCalledPhases(),
                                               containsInAnyOrder("setMuleContext",
                                                                  Initialisable.PHASE_NAME,
                                                                  Startable.PHASE_NAME));

                                    assertThat(lifecycleBean.getCalledPhases(), hasSize(5));
                                    assertThat(lifecycleBean.getCalledPhases(),
                                               containsInAnyOrder("setMuleContext",
                                                                  Initialisable.PHASE_NAME,
                                                                  Startable.PHASE_NAME,
                                                                  Stoppable.PHASE_NAME,
                                                                  Disposable.PHASE_NAME));
                                    return true;
                                  })));
  }

  @Test
  public void failOnInitialiseInvokesDisposeInOtherComponentsButNotInTheFailedOne() {
    testOnContextLifecycleFailure("lifecycle/component-failing-during-initialise-config.xml",
                                  failOnStartLifecycleBean -> {
                                    LifecycleObject lifecycleBean = failOnStartLifecycleBean.getOtherLifecycleObject();
                                    assertThat(lifecycleBean.getCalledPhases(), hasSize(3));
                                    assertThat(lifecycleBean.getCalledPhases(),
                                               containsInAnyOrder("setMuleContext",
                                                                  Initialisable.PHASE_NAME,
                                                                  Disposable.PHASE_NAME));
                                    assertThat(failOnStartLifecycleBean.getCalledPhases(), hasSize(2));
                                    assertThat(failOnStartLifecycleBean.getCalledPhases(),
                                               containsInAnyOrder("setMuleContext",
                                                                  Initialisable.PHASE_NAME));
                                  });
  }

  private void testOnContextLifecycleFailure(String configFile, Consumer<LifecycleObject> failureLifecycleBeanConsumer) {
    try {
      loadConfiguration(configFile);
      fail(EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION);
    } catch (LifecycleException e) {
      LifecycleObject lifecycleBean = (LifecycleObject) e.getComponent();
      failureLifecycleBeanConsumer.accept(lifecycleBean);
    } catch (Exception e) {
      fail(String.format("Expected a %s exception but got:\n%s", LifecycleException.class.getName(), e));
    }
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel testComponents = loadExtension(TestComponentsExtension.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.add(testComponents);

    return extensions;
  }
}
