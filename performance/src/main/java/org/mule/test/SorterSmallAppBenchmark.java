/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;


import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.config.internal.DependencyGraphLifecycleObjectSorter;
import org.mule.runtime.config.internal.registry.SpringLifecycleObjectSorter;
import org.mule.runtime.config.internal.registry.SpringRegistry;
import org.mule.runtime.config.internal.resolvers.AutoDiscoveredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DeclaredDependencyResolver;
import org.mule.runtime.config.internal.resolvers.DependencyGraphBeanDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.queue.QueueManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageExtension;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@Fork(1)
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
public class SorterSmallAppBenchmark extends FunctionalTestCase {

  @Inject
  private MuleContext muleContext;
  private ConfigurationDependencyResolver configurationDependencyResolver;
  private DeclaredDependencyResolver declaredDependencyResolver;
  private AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  private Map<String, Object> lookupObjects;
  private List<String> lookupObjectNames;
  private DependencyGraphBeanDependencyResolver resolver;
  private DependencyGraphLifecycleObjectSorter graphSorter;
  private SpringLifecycleObjectSorter springSorter;
  private final Class<?>[] allowedTypes = new Class<?>[] {
      LockFactory.class,
      ObjectStoreManager.class,
      ExpressionLanguageExtension.class,
      ExpressionLanguage.class,
      QueueManager.class,
      StreamingManager.class,
      ConfigurationProvider.class,
      Config.class,
      SecurityManager.class,
      FlowConstruct.class,
      MuleConfiguration.class,
      Initialisable.class
  };

  @Override
  protected String getConfigFile() {
    return "benchmark/small-app.xml";
  }

  @Override
  protected boolean isStartContext() {
    return false;
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Setup
  public void setUp() throws Throwable {
    setUpMuleContext();

    // Use Java Reflection API in order to get the SpringRegistry
    MuleRegistry muleRegistry = ((MuleContextWithRegistry) muleContext).getRegistry();
    Field registryField = muleRegistry.getClass().getDeclaredField("registry");
    registryField.setAccessible(true);
    SpringRegistry springRegistry = (SpringRegistry) registryField.get(muleRegistry);

    declaredDependencyResolver = new DeclaredDependencyResolver(springRegistry);
    autoDiscoveredDependencyResolver = new AutoDiscoveredDependencyResolver(springRegistry);
    configurationDependencyResolver = springRegistry.getConfigurationDependencyResolver();

    resolver = new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                                         autoDiscoveredDependencyResolver, springRegistry);
    graphSorter = new DependencyGraphLifecycleObjectSorter(resolver, allowedTypes);
    springSorter = new SpringLifecycleObjectSorter(allowedTypes, springRegistry);

    lookupObjects = springRegistry.lookupByType(Object.class);
    lookupObjectNames = new ArrayList<>();

  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppGraphSorter() {
    // add objects with a dependency graph sorter
    lookupObjects.forEach((key, value) -> {
      graphSorter.addObject(key, value);
      lookupObjectNames.add(key);
    });
    graphSorter.setLifeCycleObjectNameOrder(lookupObjectNames);

    // sort vertices
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppSpringSorter() {
    // add objects with a spring sorter (deprecated)
    lookupObjects.forEach((key, value) -> {
      springSorter.addObject(key, value);
      lookupObjectNames.add(key);
    });
    springSorter.setLifeCycleObjectNameOrder(lookupObjectNames);

    // sort vertices
    return springSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppGraphSorterGetSortedObjectsOnly() {
    return graphSorter.getSortedObjects();
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public List<Object> smallAppSpringSorterGetSortedObjectsOnly() {
    return springSorter.getSortedObjects();
  }


}
