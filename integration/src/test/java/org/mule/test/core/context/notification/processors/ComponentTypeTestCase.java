/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification.processors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentTypeStore.COMPONENT_CONFIGURATION_TYPE;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(COMPONENT_CONFIGURATION_TYPE)
public class ComponentTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/component-type-config.xml";
  }

  @Test
  public void routerComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("first-successful")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("scatter-gather")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("choice")), is(ROUTER));
    assertThat(getComponentType(buildFromStringRepresentation("round-robin")), is(ROUTER));
  }

  @Test
  public void scopeComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("try")), is(SCOPE));
    assertThat(getComponentType(buildFromStringRepresentation("until-successful")), is(SCOPE));
    assertThat(getComponentType(buildFromStringRepresentation("async")), is(SCOPE));
    assertThat(getComponentType(buildFromStringRepresentation("foreach")), is(SCOPE));
  }

  @Test
  public void operationsComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("set-payload")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("set-variable")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("remove-variable")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("parse-template")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("idempotent-message-validator")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("raise-error")), is(OPERATION));
    assertThat(getComponentType(buildFromStringRepresentation("http:request")), is(OPERATION));
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
  public void configComponentType() {
    assertThat(getComponentType(buildFromStringRepresentation("http:request-config")), is(CONFIG));
  }

  @Test
  public void sourceComponentType() {
    assertThat(getComponentType(buildFromStringRepresentation("scheduler")), is(SOURCE));
  }

  @Test
  public void sourceComponentTypeWithoutAlias() {
    assertThat(getComponentType(buildFromStringRepresentation("petstore:pet-adoption-source")), is(SOURCE));
  }

  private TypedComponentIdentifier.ComponentType getComponentType(ComponentIdentifier componentIdentifier) {
    return locator.find(componentIdentifier).get(0).getLocation().getComponentIdentifier().getType();
  }

}
