/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.test.runner.ArtifactClassLoaderRunner;

/**
 * Base {@link Class} for functional integration tests, it will run the functional test case with
 * {@link ArtifactClassLoaderRunner} in order to use the hierarchical {@link ClassLoader}'s as standalone mode. Every test on
 * integration module should extend from this base {@link Class}.
 *
 * @since 4.0
 */
public abstract class AbstractIntegrationTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  protected static final String DEFAULT_PROCESSING_STRATEGY_CLASSNAME =
      "org.mule.runtime.core.internal.processor.strategy.TransactionAwareStreamEmitterProcessingStrategyFactory";
  protected static final String PROACTOR_PROCESSING_STRATEGY_CLASSNAME =
      "org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamEmitterProcessingStrategyFactory";


  protected void setDefaultProcessingStrategyFactory(String classname) {
    setProperty(ProcessingStrategyFactory.class.getName(), classname);
  }

  protected void clearDefaultProcessingStrategyFactory() {
    clearProperty(ProcessingStrategyFactory.class.getName());
  }

}
