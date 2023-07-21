/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.test.integration.logging;

import static org.apache.logging.log4j.LogManager.setFactory;
import static org.apache.logging.log4j.LogManager.shutdown;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.log4j.internal.MuleLog4jContextFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import org.junit.rules.ExternalResource;

/**
 * Allows tests to use the mule logging infrastructure without initializing a {@link MuleContext}.
 */
public class UseMuleLog4jContextFactory extends ExternalResource {

  private static final MuleLog4jContextFactory muleLog4jContextFactory = new MuleLog4jContextFactory(true);

  private LoggerContextFactory originalLog4jContextFactory;

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
}
