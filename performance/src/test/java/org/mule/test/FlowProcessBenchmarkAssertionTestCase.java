/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareStreamEmitterProcessingStrategyFactory;

import org.junit.Test;

public class FlowProcessBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final String PROCESSING_STRATEGY_PARAM = "processingStrategyFactory";

  @Test
  public void processStreamOf1000FlowDefault() {
    runAndAssertBenchmark(FlowNullProcessorBenchmark.class, "processSourceStream", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {
                                           TransactionAwareStreamEmitterProcessingStrategyFactory.class
                                               .getCanonicalName()}),
                          9, MILLISECONDS, 7800000);
  }


  @Test
  public void processStreamOf1000FlowSynchronous() {
    runAndAssertBenchmark(FlowNullProcessorBenchmark.class, "processSourceStream", 1,
                          singletonMap(PROCESSING_STRATEGY_PARAM,
                                       new String[] {BlockingProcessingStrategyFactory.class.getCanonicalName()}),
                          9, MILLISECONDS, 7800000);
  }


}
