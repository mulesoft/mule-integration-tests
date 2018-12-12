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
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;

public class CustomTestComponent implements Initialisable, Disposable, Processor {

  private Closeable internalState;

  @Override
  public void initialise() throws InitialisationException {
    internalState = new StringReader("content");
  }

  @Override
  public void dispose() {
    try {
      internalState.close();
    } catch (IOException e) {
      // Nothing to do...
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return event;
  }
}
