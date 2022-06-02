/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.functional.AbstractXmlExtensionMuleArtifactFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import org.junit.runners.Parameterized;

@Features({@Feature(XML_SDK), @Feature(LAZY_INITIALIZATION)})
@RunnerDelegateTo(Parameterized.class)
public class LazySmartConnectorFlowRefTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Parameterized.Parameter
  public String configFile;

  @Parameterized.Parameter(1)
  public String moduleFile;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  public static Collection<Object[]> data() {
    return asList(new Object[] {"flows/flows-with-module-using-flow-ref.xml", "modules/module-using-flow-ref.xml"},
                  new Object[] {"flows/flows-with-module-with-config-using-flow-ref.xml",
                      "modules/module-with-config-using-flow-ref.xml"});
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Override
  protected String getModulePath() {
    return moduleFile;
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
  @Issue("W-11177824")
  @Description("Verify that flows defined in the XML SDK connector definition are found when doing lazy init")
  public void findInternalFlows() {
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("invoke-call-flow").build());
  }

  public static class Util {

    public void util() {

    }
  }
}
