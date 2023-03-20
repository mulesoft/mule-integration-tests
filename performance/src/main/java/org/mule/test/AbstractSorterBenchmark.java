/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;


import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@Fork(1)
@State(Scope.Benchmark)
@OutputTimeUnit(MILLISECONDS)
public class AbstractSorterBenchmark extends FunctionalTestCase {

  @Inject
  private MuleContext muleContext;
  protected SpringRegistry springRegistry;
  protected ConfigurationDependencyResolver configurationDependencyResolver;
  protected DeclaredDependencyResolver declaredDependencyResolver;
  protected AutoDiscoveredDependencyResolver autoDiscoveredDependencyResolver;
  protected Map<String, Object> lookupObjects;
  protected DependencyGraphBeanDependencyResolver resolver;
  protected DependencyGraphLifecycleObjectSorter graphSorter;
  protected SpringLifecycleObjectSorter springSorter;

  public static final Class<?>[] allowedTypes = new Class<?>[] {
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

  @Setup
  public void setUp() throws Throwable {
    setUpMuleContext();

    springRegistry = getSpringRegistry(getMuleContext());

    declaredDependencyResolver = new DeclaredDependencyResolver(springRegistry);
    autoDiscoveredDependencyResolver = new AutoDiscoveredDependencyResolver(springRegistry);
    configurationDependencyResolver = springRegistry.getConfigurationDependencyResolver();

    resolver = new DependencyGraphBeanDependencyResolver(configurationDependencyResolver, declaredDependencyResolver,
                                                         autoDiscoveredDependencyResolver, springRegistry);

    lookupObjects = springRegistry.lookupByType(Object.class);

    graphSorter = getGraphSorter(resolver);
    addObjectsToSorter(graphSorter, lookupObjects);
    springSorter = getSpringSorter(springRegistry);
    addObjectsToSorter(springSorter, lookupObjects);
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  protected boolean isStartContext() {
    return false;
  }

  protected boolean doTestClassInjection() {
    return true;
  }

  protected SpringRegistry getSpringRegistry(MuleContext muleContext) throws NoSuchFieldException, IllegalAccessException {
    // Use Java Reflection API in order to get the SpringRegistry
    MuleRegistry muleRegistry = ((MuleContextWithRegistry) muleContext).getRegistry();
    Field registryField = muleRegistry.getClass().getDeclaredField("registry");
    registryField.setAccessible(true);
    return (SpringRegistry) registryField.get(muleRegistry);
  }

  protected DependencyGraphLifecycleObjectSorter getGraphSorter(DependencyGraphBeanDependencyResolver resolver) {
    return new DependencyGraphLifecycleObjectSorter(resolver, allowedTypes);
  }

  protected SpringLifecycleObjectSorter getSpringSorter(SpringRegistry springRegistry) {
    return new SpringLifecycleObjectSorter(allowedTypes, springRegistry);
  }

  protected void addObjectsToSorter(LifecycleObjectSorter sorter, Map<String, Object> lookupObjects) {
    List<String> lookupObjectNames = new ArrayList<>();
    // add objects with a dependency graph sorter
    lookupObjects.forEach((key, value) -> {
      sorter.addObject(key, value);
      lookupObjectNames.add(key);
    });
    sorter.setLifeCycleObjectNameOrder(lookupObjectNames);
  }

}

