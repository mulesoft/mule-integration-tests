/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;


import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SorterSmallAppBenchmark extends AbstractSorterBenchmark {

  @Override
  protected String getConfigFile() {
    return "benchmark/small-app.xml";
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppGraphSorter() {
    // add objects
    LifecycleObjectSorter graphSorter = getGraphSorter(resolver);
    addObjectsToSorter(graphSorter, lookupObjects);

    // sort objects
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public void smallAppGraphSorterAddObjectsOnly() {
    // add objects
    LifecycleObjectSorter graphSorter = getGraphSorter(resolver);
    addObjectsToSorter(graphSorter, lookupObjects);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppGraphSorterGetSortedObjectsOnly() {
    // sort objects
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppSpringSorter() {
    // add objects
    LifecycleObjectSorter springSorter = getSpringSorter(springRegistry);
    addObjectsToSorter(springSorter, lookupObjects);

    // sort objects
    return springSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public void smallAppSpringSorterAddObjectsOnly() {
    // add objects
    LifecycleObjectSorter springSorter = getSpringSorter(springRegistry);
    addObjectsToSorter(springSorter, lookupObjects);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppSpringSorterGetSortedObjectsOnly() {
    // sort objects
    return springSorter.getSortedObjects();
  }

}
