/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(XML_SDK)
@Issue("W-12362157")
public class XmlSdkConfigDeploymentTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

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
  @Description("This test is testing that an app with an xml-sdk config and a lot of request configs deploys in a timely manner."
      + "If this test timeouts, it may be because it got stuck initializing the beans."
      + "During the initialization of the spring context, Spring checks the object type of the beans definition list to"
      + "see if that factory class returns an object that could be injected in one of their fields. "
      + "If in that factory the ObjectTypeClass field is not available right after instantiation, "
      + "like in [ObjectFactoryClassRepositoryTestCase#getObjectTypeWithoutInitializingTheFields] or in"
      + "[ObjectFactoryClassRepositoryTestCase#testGetObjectTypeReturnsSuperIfImplementsObjectTypeProvider],"
      + "Spring will have to fully initialize the bean, resulting in a performance issue where the"
      + "apps may never finish deploying")
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
