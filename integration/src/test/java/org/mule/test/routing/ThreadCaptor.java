/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Set;

public class ThreadCaptor extends AbstractComponent implements Processor, Initialisable, Disposable {

  private static Set<Thread> capturedThreads;

  @Override
  public void initialise() throws InitialisationException {
    setCapturedThreads(newKeySet());
  }

  @Override
  public void dispose() {
    setCapturedThreads(null);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    capturedThreads.add(currentThread());
    if (capturedThreads.size() > 2) {
      Latch latch = (Latch) event.getVariables().get("latch").getValue();
      if (latch != null) {
        latch.release();
      }
    }

    return event;
  }

  private static void setCapturedThreads(Set<Thread> capturedThreads) {
    ThreadCaptor.capturedThreads = capturedThreads;
  }

  public static Set<Thread> getCapturedThreads() {
    return capturedThreads;
  }
}
