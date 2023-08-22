/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

/**
 * {@link Processor} that throws exceptions based on the presence of an "exception" variable in the incoming {@link CoreEvent}.
 *
 * @since 4.1.0
 */
public class ThrowExceptionProcessor implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    Throwable exception = (Throwable) event.getVariables().get("exception").getValue();
    if (exception instanceof MuleException) {
      throw (MuleException) exception;
    } else if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    }
    return event;
  }

}
