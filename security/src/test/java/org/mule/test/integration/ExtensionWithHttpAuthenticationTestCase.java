/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Types.TYPE_CATALOG;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(TYPE_CATALOG)
public class ExtensionWithHttpAuthenticationTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/extension-with-http-authentication-config.xml";
  }

  @Test
  @Issue("MULE-XXXXX")
  public void authenticationDataProperlyPopulated() throws Exception {
    Map<String, String> authParams = (Map<String, String>) flowRunner("getAuthenticationData")
        .run().getMessage().getPayload().getValue();

    System.out.println(authParams);
  }

}
