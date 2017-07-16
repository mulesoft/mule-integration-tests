/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.INTERCEPTING;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentTypeStore.COMPONENT_CONFIGURATION_TYPE;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(COMPONENT_CONFIGURATION_TYPE)
public class ComponentTypeTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ConfigurationComponentLocator configurationComponentLocator;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/component-type-config.xml";
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Test
  public void routerComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("until-successful")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("first-successful")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("foreach")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("scatter-gather")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("choice")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("round-robin")), is(ROUTER));
  }

  @Test
  public void scopeComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("try")), is(SCOPE));
    assertThat(getComponentType(buildFromStringRepresentation("enricher")), is(SCOPE));
    assertThat(getComponentType(buildFromStringRepresentation("async")), is(SCOPE));
  }

  @Test
  public void interceptingComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("splitter")), is(INTERCEPTING));
    assertThat(getComponentType(buildFromStringRepresentation("resequencer")), is(INTERCEPTING));
  }

  @Test
  public void processorComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("set-payload")), is(PROCESSOR));
    assertThat(getComponentType(buildFromStringRepresentation("set-variable")), is(PROCESSOR));
  }

  @Test
  public void operationsComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("http:request")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("module-using-core:set-payload-hardcoded")), is(OPERATION));
  }

  @Test
  public void errorComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("error-handler")), is(ERROR_HANDLER));
    assertThat(getComponentType(buildFromStringRepresentation("on-error-continue")), is(ON_ERROR));
  }

  @Test
  public void flowComponentType() {
    assertThat(getComponentType(buildFromStringRepresentation("flow")), is(FLOW));
  }

  @Test
  public void unknownComponentType() {
    assertThat(getComponentType(buildFromStringRepresentation("http:request-config")), is(UNKNOWN));
  }

  @Test
  public void sourceComponentType() {
    assertThat(getComponentType(buildFromStringRepresentation("scheduler")), is(SOURCE));
  }

  private TypedComponentIdentifier.ComponentType getComponentType(ComponentIdentifier componentIdentifier) {
    return configurationComponentLocator.find(componentIdentifier).get(0).getLocation().getComponentIdentifier().getType();
  }

}
