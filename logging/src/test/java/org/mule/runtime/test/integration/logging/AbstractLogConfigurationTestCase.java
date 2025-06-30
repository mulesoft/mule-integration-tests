/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class AbstractLogConfigurationTestCase extends AbstractFakeMuleServerTestCase {

  protected boolean loggerHasAppender(String appName, Logger logger, String appenderName) throws Exception {
    return getContext(appName).getConfiguration().getLoggerConfig(logger.getName()).getAppenders().containsKey(appenderName);
  }

  protected Logger getRootLoggerForApp(String appName) throws Exception {
    return getContext(appName).getLogger("");
  }

  private LoggerContext getContext(final String appName) throws Exception {
    return withAppClassLoader(appName, () -> {
      Application app = muleServer.findApplication(appName);
      ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
      return (LoggerContext) LogManager.getContext(classLoader, false);
    });
  }

  protected <T> T withAppClassLoader(String appName, Callable<T> closure) throws Exception {
    Application app = muleServer.findApplication(appName);
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return closure.call();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  protected List<Appender> selectByClass(String appName, Class<?> appenderClass) throws Exception {
    LoggerContext context = getContext(appName);
    List<Appender> filteredAppenders = new LinkedList<>();
    for (Appender appender : context.getConfiguration().getAppenders().values()) {
      if (appenderClass.isAssignableFrom(appender.getClass())) {
        filteredAppenders.add(appender);
      }
    }

    return filteredAppenders;
  }

}
