/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import io.qameta.allure.Issue;
import org.junit.Test;

// It's necessary to have the Spring dependencies as shared libraries so the spring-module can see them.
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.springframework.security:spring-security-core",
    "org.springframework:spring-core",
    "org.springframework:spring-context",
    "org.springframework:spring-beans",
    "org.springframework:spring-expression",
    "org.springframework:spring-aop"
})
@Issue("W-15832941")
public class SpringAppWithBeanDependingOnExtensionsClientTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "apps/app-with-bean-depending-on-extensions-client.xml";
  }

  @Test
  public void whenBeanDependsOnExtensionsClientThenItCanCallOperation() throws Exception {
    CoreEvent result = runFlow("mainFlow");
    assertThat(result, hasMessage(hasPayload(is("tasty Apple"))));
  }
}
