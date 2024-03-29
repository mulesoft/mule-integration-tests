/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;

public class FailLifecycleTestObject implements Initialisable, Disposable {

  private static boolean initInvoked = false;
  private static boolean disposeInvoked = false;

  @Override
  public void initialise() throws InitialisationException {
    initInvoked = true;
    throw new InitialisationException(new RuntimeException(), this);
  }

  @Override
  public void dispose() {
    disposeInvoked = true;
  }

  public static boolean isInitInvoked() {
    return initInvoked;
  }

  public static boolean isDisposeInvoked() {
    return disposeInvoked;
  }

  public static void setup() {
    initInvoked = false;
    disposeInvoked = false;
  }
}
