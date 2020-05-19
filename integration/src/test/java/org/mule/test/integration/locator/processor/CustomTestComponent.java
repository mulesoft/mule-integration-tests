/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator.processor;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class CustomTestComponent implements Initialisable, Startable, Stoppable, Disposable, Processor {

  public static Map<CustomTestComponent, String> statesByInstances = new HashMap<>();

  private Closeable value;

  @Override
  public void initialise() {
    statesByInstances.put(this, statesByInstances.getOrDefault(this, "initialized"));
    value = new StringReader("content");
  }

  @Override
  public void start() {
    statesByInstances.put(this, statesByInstances.get(this) + "_started");
  }

  @Override
  public void stop() {
    statesByInstances.put(this, statesByInstances.get(this) + "_stopped");
  }

  @Override
  public void dispose() {
    try {
      value.close();
    } catch (IOException e) {
      // Nothing to do...
    } finally {
      statesByInstances.put(this, statesByInstances.get(this) + "_disposed");
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return event;
  }
}
