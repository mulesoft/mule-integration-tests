/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.functional.AbstractXmlExtensionMuleArtifactFunctionalTestCase;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;

@Features({@Feature(XML_SDK), @Feature(LAZY_INITIALIZATION)})
public class LazySmartConnectorErrorTypeRepository extends AbstractXmlExtensionMuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-errors.xml";
  }

  @Override
  protected String getModulePath() {
    return "modules/module-using-errors.xml";
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
  @Issue("MULE-19241")
  @Description("Verify that error mappings in XML SDK connector operations are not added more than once to the errorTypeRepository")
  public void noDuplicateErrorTypes() {
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("invoke-mapping").build());
    assertThat(muleContext.getErrorTypeRepository()
        .getErrorType(buildFromStringRepresentation("MODULE-USING-ERRORS:SOME_ERROR")).isPresent(),
               is(true));
  }

  public static class Util {

    public void util() {

    }
  }
}
