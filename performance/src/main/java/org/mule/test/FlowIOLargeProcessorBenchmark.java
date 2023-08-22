/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.Mono;

@OutputTimeUnit(MILLISECONDS)
public class FlowIOLargeProcessorBenchmark extends AbstractFlowBenchmark {

  @Override
  protected List<Processor> getMessageProcessors() {
    return singletonList(iorwLarge);
  }

  @Override
  protected int getStreamIterations() {
    return 1000;
  }


}
