/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_BYTE_BUDDY_OBJECT_CREATION_PROPERTY;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

@Feature(XML_SDK)
@Issue("W-12362157")
public class XmlSdkConfigDeploymentTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty enableByteBuddy = new SystemProperty(ENABLE_BYTE_BUDDY_OBJECT_CREATION_PROPERTY, "true");

  @Override
  protected String getModulePath() {
    return "modules/module-test-connection-multiple-connectors-uses-first.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-test-connection-modules-with-additional-configs.xml";
  }

  @Override
  protected boolean shouldValidateXml() {
    return true;
  }

  @Test
  public void testDeployment() throws Exception {
    assertConfigPresent("theConfigurationNameFromTheAppThatWontBeMacroExpanded");
    assertConfigPresent("anotherConfigurationToShowThereIsNoClashOnMacroExpansion");
  }

  private void assertConfigPresent(String beanName) throws MuleException {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration(beanName, testEvent());
    assertThat(config, is(notNullValue()));
    assertThat(config.getConnectionProvider().isPresent(), is(true));
  }
}
