/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.List;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@OutputTimeUnit(MILLISECONDS)
public class FlowLightMixProcessorBenchmark extends AbstractFlowBenchmark {

  @Override
  protected List<Processor> getMessageProcessors() {
    List<Processor> processors = new ArrayList<>();
    processors.add(cpuLightProcessor);
    processors.add(cpuLightProcessor);
    processors.add(iorwMedium);
    processors.add(cpuLightProcessor);
    processors.add(cpuLightProcessor);
    return processors;
  }

  @Override
  protected int getStreamIterations() {
    return 100;
  }

}
