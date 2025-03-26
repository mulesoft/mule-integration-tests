/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class PolicyCacheDisposalTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/policy-cache-disposal-config.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Description("Start and stop flow source")
  @Test
  public void policyCacheEntriesGetEvictedOnFlowDisposal() throws Exception {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("listenerFlow")
        || componentLocation.getLocation().equals("hitFlow"));
    CoreEvent hitFlow = flowRunner("hitFlow").keepStreamsOpen().run();
    assertThat(IOUtils.toString((CursorStreamProvider) hitFlow.getMessage().getPayload().getValue()), equalTo("Hello"));
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("listenerFlow")
        || componentLocation.getLocation().equals("hitFlow"));
    CoreEvent hitFlow2 = flowRunner("hitFlow").keepStreamsOpen().run();
    assertThat(IOUtils.toString((CursorStreamProvider) hitFlow2.getMessage().getPayload().getValue()), equalTo("Hello"));
  }

}
