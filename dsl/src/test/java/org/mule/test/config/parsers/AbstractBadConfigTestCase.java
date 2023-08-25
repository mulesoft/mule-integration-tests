/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.parsers;

import static org.hamcrest.Matchers.containsString;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.MuleContext;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * A stripped-down version of FunctionalTestCase that allows us to test the parsing of a bad configuration.
 */
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.mule.tests:mule-derby-all",
    "org.mule.tests:mule-activemq-broker"})
public abstract class AbstractBadConfigTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Override
  protected boolean doTestClassInjection() {
    return false;
  }

  @Override
  protected MuleContext createMuleContext() throws Exception {
    return null;
  }

  public void assertErrorContains(String phrase) throws Exception {
    expected.expectMessage(containsString(phrase));

    parseConfig();
  }

  protected void parseConfig() throws Exception {
    super.createMuleContext();
  }

}
