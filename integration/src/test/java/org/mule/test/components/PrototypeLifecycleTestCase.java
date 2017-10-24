/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class PrototypeLifecycleTestCase extends AbstractIntegrationTestCase {

  private final static Map<LifecycleTracker, Map<String, AtomicInteger>> trackers = new HashMap<>();

  @Override
  protected String getConfigFile() {
    return "prototype-lifecycle-config.xml";
  }

  @Test
  public void lifecycleNotAppliedTwiceWhenAskedForInRegistry() throws Exception {

    //run the flow so that sub-flow gets initialised
    flowRunner("mainFlow").run();

    assertThat(trackers.size(), is(1));
    assertCountersAreAllOne();


    //Ask for the flow in the registry and check that the tracker is different and no one was applied the lifecycle twice
    Object subFlow = registry.lookupByName("subFlow").get();

    assertThat(trackers.size(), is(2)); //There should be 2 different trackers
    assertCountersAreAllOne();

    //Do the same asking for the object in the registry with a different method
    Collection components = registry.lookupAllByType(ExecutableComponent.class);
    Component subFlow2 = (Component) components.stream().filter(component -> !(component instanceof Flow)).findFirst().get();

    //Check that both subFlows are of the same type
    assertThat(subFlow, is(instanceOf(subFlow2.getClass())));
    assertThat(subFlow2, is(instanceOf(subFlow.getClass())));

    //Check that subflow found is different from the previous one
    assertThat(subFlow, is(not(sameInstance(subFlow2))));

    //Check that no subflow was applied a lifecycle phase more than once.
    assertThat(trackers.size(), is(3));
    assertCountersAreAllOne();

  }

  private void assertCountersAreAllOne() {
    trackers.values().forEach(trackingMap -> trackingMap.values().forEach(counter -> assertThat(counter.get(), is(1))));
  }


  public static class LifecycleTracker extends AbstractComponent implements Processor, Initialisable, Startable {

    @Override
    public void initialise() throws InitialisationException {
      addToMap();
      incrementLifecycleCount(Initialisable.PHASE_NAME);
    }

    @Override
    public void start() throws MuleException {
      addToMap();
      incrementLifecycleCount(Startable.PHASE_NAME);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }

    private void addToMap() {
      if (!trackers.containsKey(this)) {
        trackers.put(this, new HashMap<>());
      }
    }

    private void incrementLifecycleCount(String phase) {
      Map<String, AtomicInteger> myMap = trackers.get(this);
      if (myMap.containsKey(phase)) {
        myMap.get(phase).getAndIncrement();
      } else {
        AtomicInteger newCounter = new AtomicInteger();
        newCounter.getAndIncrement();
        myMap.put(phase, newCounter);
      }
    }
  }

}
