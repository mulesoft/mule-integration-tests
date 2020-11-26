/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import org.junit.Before;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
@RunnerDelegateTo(Parameterized.class)
public class GracefulShutdownTestCase extends AbstractIntegrationTestCase {

  protected static Latch waitLatch;
  protected static Latch contextStopLatch;

  @Parameters(name = "{0}")
  public static List<String> parameters() {
    return asList("org/mule/shutdown/flow-with-scatter-gather-and-flow-ref.xml");
  }

  @Rule
  public SystemProperty propagateDisposeError = new SystemProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR, "");

  private final String configFile;

  public GracefulShutdownTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Before
  public void setUpWaitLatch() throws Exception {
    waitLatch = new Latch();
    contextStopLatch = new Latch();
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  @Description("Verify that the graceful shutdown occurs in a timely manner")
  public void flowStopTimelyManner() throws Exception {
    for (int i = 0; i < getRuntime().availableProcessors() * 4; ++i) {
      flowRunner("flow").run();
    }
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  public static class BlockMessageProcessor extends AbstractComponent implements Processor {
    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return sleepFor(event, 1000);
    }
  }

}
