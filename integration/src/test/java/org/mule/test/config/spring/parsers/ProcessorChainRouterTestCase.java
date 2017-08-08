/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.config.dsl.ParsersPluginTest;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.junit.Test;

public class ProcessorChainRouterTestCase extends AbstractIntegrationTestCase implements ParsersPluginTest {

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/processor-chain-router-config.xml";
  }

  @Test
  public void compositeProcessorChainRouter() throws Exception {
    Object chainRouter =
        componentLocator.find(Location.builder().globalName("compositeChainRouter").build()).get();
    Event event = Event.builder().message(Message.builder().value("testPayload").build())
        .addVariable("customVar", "Value").build();

    // TODO MULE-13132 - we need to call this method using reflection because there's no support for being able to access
    // privileged API classes.
    Method method = ClassUtils.getMethod(chainRouter.getClass(), "process", new Class[] {Event.class});
    Event returnedEvent = (Event) method.invoke(chainRouter, event);
    assertThat(returnedEvent.getMessage().getPayload().getValue(), is("testPayload custom"));
    assertThat(returnedEvent.getVariables().get("myVar").getValue(), is("myVarValue"));
    assertThat(returnedEvent.getVariables().get("mySecondVar").getValue(), is("mySecondVarValue"));
    assertThat(returnedEvent.getVariables().get("myThirdVar").getValue(), is("myThirdVarValue"));
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }
}
