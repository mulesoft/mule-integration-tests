/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.tck.junit4.rule.VerboseExceptions.setVerboseExceptions;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.ERROR_REPORTING;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.ClassRule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(LOGGING), @Feature(XML_SDK)})
@Story(ERROR_REPORTING)
public class LogCheckXmlModuleTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static VerboseExceptions verboseExceptions = new VerboseExceptions(false);

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/log-check-xml-module-config.xml";
  }

  @Test
  public void runXmlSdkOperationError() throws Exception {
    runSuccesses(true, "xmlSdkOperationError");
  }

  @Test
  public void runXmlSdkOperationErrorNested() throws Exception {
    runSuccesses(true, "xmlSdkOperationErrorNested");
  }

  private void runSuccesses(boolean verboseExceptions, String flowName) throws Exception {
    setVerboseExceptions(verboseExceptions);
    flowRunner(flowName).run();
  }

}
