/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package logging;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JclLoggerClient implements Processor {

  private static final Log jclLogger = LogFactory.getLog(JclLoggerClient.class);

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    jclLogger.error("My logger is JCL");
    return event;
  }

}