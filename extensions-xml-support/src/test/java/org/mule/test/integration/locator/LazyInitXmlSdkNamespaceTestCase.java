/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.functional.AbstractXmlExtensionMuleArtifactFunctionalTestCase;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import org.junit.Test;

@Features({@Feature(XML_SDK), @Feature(LAZY_INITIALIZATION)})
public class LazyInitXmlSdkNamespaceTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "flows/flow-with-module-using-flow-ref.xml";
  }

  @Override
  protected String[] getModulePaths() {
    return new String[] {"modules/module-using-flow-ref-with-error-mapping-operation.xml"};
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Test
  @Issue("W-14559071")
  @Description("Using the same namespace with the current module/extension's namespace shouldn't throw an error.")
  public void errorMappingWithoutNamespaceError() {
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("test-flow").build());
  }

}
