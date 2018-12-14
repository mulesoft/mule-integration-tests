/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.flow;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class FlowProcessingStrategyConfigTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/spring/flow/flow-processing-strategies.xml";
  }

  @Test
  public void testDefault() throws Exception {
    assertThat(getFlowProcessingStrategy("defaultFlow").getClass().getName(),
               equalTo("org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamEmitterProcessingStrategyFactory$TransactionAwareProactorStreamEmitterProcessingStrategy"));
  }

  @Test
  public void testDefaultAsync() throws Exception {
    assertThat(getFlowProcessingStrategy("defaultAsync").getClass().getName(),
               equalTo("org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamEmitterProcessingStrategyFactory$TransactionAwareProactorStreamEmitterProcessingStrategy"));
  }

  private ProcessingStrategy getFlowProcessingStrategy(String flowName) throws Exception {
    return registry.<Flow>lookupByName(flowName).get().getProcessingStrategy();
  }
}
