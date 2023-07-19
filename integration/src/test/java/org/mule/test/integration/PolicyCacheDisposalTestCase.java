/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;

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
