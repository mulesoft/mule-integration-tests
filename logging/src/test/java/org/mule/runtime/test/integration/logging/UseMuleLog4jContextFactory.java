/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.runtime.module.log4j.internal.MuleLog4jConfiguratorUtils.configureSelector;

import static org.apache.logging.log4j.LogManager.setFactory;
import static org.apache.logging.log4j.LogManager.shutdown;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.rules.ExternalResource;

/**
 * Allows tests to use the mule logging infrastructure without initializing a {@link MuleContext}.
 */
public class UseMuleLog4jContextFactory extends ExternalResource {

  private final MuleLog4jContextFactory muleLog4jContextFactory;

  private LoggerContextFactory originalLog4jContextFactory;

  public UseMuleLog4jContextFactory() {
    this(null);
  }

  public UseMuleLog4jContextFactory(ContextSelector selector) {
    this.muleLog4jContextFactory = getContextFactory(selector);
  }

  @Override
  protected void before() {
    originalLog4jContextFactory = LogManager.getFactory();
    setFactory(muleLog4jContextFactory);
  }

  @Override
  protected void after() {
    // We can safely force a removal of the old logger contexts instead of waiting for the reaper thread to do it.
    ((MuleLog4jContextFactory) LogManager.getFactory()).dispose();
    setFactory(originalLog4jContextFactory);
    shutdown();
  }

  private static MuleLog4jContextFactory createContextFactory() {
    MuleLog4jContextFactory contextFactory = new MuleLog4jContextFactory();
    configureSelector(contextFactory, true);
    return contextFactory;
  }

  private static MuleLog4jContextFactory getContextFactory(ContextSelector selector) {
    MuleLog4jContextFactory muleLog4jContextFactory = createContextFactory();
    if (selector != null) {
      configureSelector(muleLog4jContextFactory, selector);
    }
    return muleLog4jContextFactory;
  }

}
