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
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentTypeStore.COMPONENT_CONFIGURATION_TYPE;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(XML_SDK), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(COMPONENT_CONFIGURATION_TYPE)
public class ComponentTypeTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/component-type-config.xml";
  }

  @Test
  public void operationsComponentTypes() {
    assertThat(getComponentType(buildFromStringRepresentation("module-using-core:set-payload-hardcoded")), is(OPERATION));
  }

  private TypedComponentIdentifier.ComponentType getComponentType(ComponentIdentifier componentIdentifier) {
    return locator.find(componentIdentifier).get(0).getLocation().getComponentIdentifier().getType();
  }

}
