/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
public class SorterLargeAppBenchmark extends AbstractSorterBenchmark {

  @Override
  protected String getConfigFile() {
    return "benchmark/large-app.xml";
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> largeAppGraphSorter() {
    // add objects
    LifecycleObjectSorter graphSorter = getGraphSorter(resolver);
    addObjectsToSorter(graphSorter, lookupObjects);

    // sort objects
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public void largeAppGraphSorterAddObjectsOnly() {
    // add objects
    LifecycleObjectSorter graphSorter = getGraphSorter(resolver);
    addObjectsToSorter(graphSorter, lookupObjects);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> largeAppGraphSorterGetSortedObjectsOnly() {
    // sort objects
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> largeAppSpringSorter() {
    // add objects
    LifecycleObjectSorter springSorter = getSpringSorter(springRegistry);
    addObjectsToSorter(springSorter, lookupObjects);

    // sort objects
    return springSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public void largeAppSpringSorterAddObjectsOnly() {
    // add objects
    LifecycleObjectSorter springSorter = getSpringSorter(springRegistry);
    addObjectsToSorter(springSorter, lookupObjects);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> largeAppSpringSorterGetSortedObjectsOnly() {
    // sort objects
    return springSorter.getSortedObjects();
  }

}
