/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(MICROSECONDS)
public class FlowCPULightProcessorBenchmark extends AbstractFlowBenchmark {

  @Override
  protected List<Processor> getMessageProcessors() {
    return singletonList(cpuLightProcessor);
  }

  @Override
  protected int getStreamIterations() {
    return 1000;
  }

}
