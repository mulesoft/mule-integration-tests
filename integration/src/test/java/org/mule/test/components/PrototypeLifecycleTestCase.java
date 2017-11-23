/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
