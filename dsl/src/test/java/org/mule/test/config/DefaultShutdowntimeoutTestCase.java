/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.mule.runtime.api.util.MuleSystemProperties.GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.apache.derby:derby",
    "org.apache.activemq:activemq-client",
    "org.apache.activemq:activemq-broker",
    "org.apache.activemq:activemq-kahadb-store"})
@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class DefaultShutdowntimeoutTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty gracefulShutdownDefaultTimeout = new SystemProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT, "1234");

  @Override
  protected String getConfigFile() {
    return "./simple.xml";
  }

  @Test
  @Description("Verify that the dsl/extModel do not have a default value that overrides the one from the environment configuration.")
  public void defaultShutdownTimeoutOverride() {
    assertThat(muleContext.getConfiguration().getShutdownTimeout(), is(1234L));
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
