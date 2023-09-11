/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.routing;

import io.qameta.allure.Issue;
import org.mule.test.performance.util.AbstractIsolatedFunctionalPerformanceTestCase;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-11450: Migrate Contiperf tests to JMH")
@Issue("MULE-11450")
public class ScatterGatherRouterPerformanceTestCase extends AbstractIsolatedFunctionalPerformanceTestCase {

  @Override
  protected String getConfigFile() {
    return "scatter-gather-perf-test.xml";
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Required(throughput = 180, average = 6, percentile90 = 7)
  public void parallelProcessing() throws Exception {
    this.runFlow("parallelProcessing");
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  @Required(throughput = 220, average = 5, percentile90 = 6)
  public void parallelHttpProcessing() throws Exception {
    this.runFlow("parallelHttpProcessing");
  }

  @Test
  @PerfTest(duration = 15000, threads = 10, warmUp = 5000)
  @Required(throughput = 1600, average = 6, percentile90 = 13)
  public void parallelHttMultiThreadedProcessing() throws Exception {
    this.runFlow("parallelHttpProcessing");
  }

  @Test
  @PerfTest(duration = 15000, threads = 10, warmUp = 5000)
  @Required(throughput = 2000, average = 5, percentile90 = 6)
  public void sequentialHttpMultiThreadedProcessing() throws Exception {
    this.runFlow("sequentialHttpProcessing");
  }
}
