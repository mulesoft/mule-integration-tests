/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ConnectivityTestingStory.CONNECTIVITY_TESTING_SERVICE;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(XML_SDK), @Feature(SDK_TOOLING_SUPPORT)})
@Story(CONNECTIVITY_TESTING_SERVICE)
public class LazySmartConnectorUsesAppConfigTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  @Named(CONNECTIVITY_TESTING_SERVICE_KEY)
  private ConnectivityTestingService connectivityTestingService;

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/lifecycle/smart-connector-using-file-config.xml";
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
  public void testConnection() {
    ConnectionValidationResult result =
        connectivityTestingService.testConnection(Location.builder().globalName("fileConfig").build());
    assertThat(result.isValid(), is(true));
  }
}
