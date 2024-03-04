/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.client.deprecated;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.oauth.api.http.HttpClientFactory;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

/**
 * A dummy connection provider that will simulate creating an OAuth Dancer extending from
 * {@link org.mule.runtime.oauth.internal.AbstractOAuthDancer}.
 * <p>
 * It will also start the dancer to detect interoperability issues between classes coming from different ClassLoaders.
 */
public class TestOAuthConnectionProvider implements org.mule.runtime.api.connection.ConnectionProvider<TestOAuthConnection> {

  @Inject
  private SchedulerService schedulerService;

  @Inject
  protected HttpService httpService;

  @Override
  public TestOAuthConnection connect() throws ConnectionException {
    // We don't really need to connect to anything real for this test, we just need the dancer to be started successfully and
    // simulate a token refresh with the minimal configuration possible.
    TestOAuthDancerBuilder builder = new TestOAuthDancerBuilder(schedulerService,
                                                                obj -> new ReentrantLock(true),
                                                                new HashMap<>(),
                                                                HttpClientFactory.getDefault(httpService),
                                                                null);
    builder.tokenUrl("http://localhost");
    TestOAuthDancer dancer = builder.build();
    try {
      LifecycleUtils.initialiseIfNeeded(dancer);
      LifecycleUtils.startIfNeeded(dancer);
    } catch (MuleException e) {
      throw new ConnectionException(e);
    }

    return new TestOAuthConnection();
  }

  @Override
  public void disconnect(TestOAuthConnection connection) {
    // Not needed for this test.
  }

  @Override
  public ConnectionValidationResult validate(TestOAuthConnection connection) {
    // Not needed for this test.
    return null;
  }
}
