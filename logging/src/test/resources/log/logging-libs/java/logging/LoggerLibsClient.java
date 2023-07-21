/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

public class LoggerLibsClient implements Processor {

  private static final Logger slf4jLogger = LoggerFactory.getLogger(LoggerLibsClient.class);
  private static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(LoggerLibsClient.class.getName());
  private static final org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(LoggerLibsClient.class);
  private static final Log jclLogger = LogFactory.getLog(LoggerLibsClient.class);

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    slf4jLogger.error("My logger is SLF4J");
    julLogger.severe("My logger is JUL");
    log4jLogger.error("My logger is LOG4J");
    jclLogger.error("My logger is JCL");
    return event;
  }

}