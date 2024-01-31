/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_APP_MODE_PROPERTY;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.rules.RuleChain.outerRule;
import static org.slf4j.LoggerFactory.getLogger;

import io.qameta.allure.Description;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.FileNotContainInLine;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;

@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppModeLoggerReconfigurationOnMuleAppDeploymentTestCase extends AbstractFakeMuleServerTestCase {

  public static final String MESSAGE_BEFORE_APP_DEPLOYMENT = "Message before app deployment.";
  public static final String MESSAGE_AFTER_APP_DEPLOYMENT = "Message after app deployment.";

  public static final String LOGGING_APP_NAME = "logging-app";
  public static final String TEST_LOG_FILE = "test.log";
  public static final String APP_XML_FILE_NAME = "log/logging-libs/logging-libs-app.xml";

  // This guarantees order of rule execution.
  @Rule
  public TestRule chain = outerRule(new SystemProperty(SINGLE_APP_MODE_PROPERTY, "true"))
      .around(new UseMuleLog4jContextFactory());

  @Test
  @Description("After an app is deployed in single app mode, all the logs go to the config of the app. " +
      "In this case the tests classloader logs works as the container log.")
  public void afterAppDeploymentAllLogsAreSentToAppendersAccordingToAppLogConfig() throws Exception {
    muleServer.start();

    Logger logger = getLogger(SingleAppModeLoggerReconfigurationOnMuleAppDeploymentTestCase.class);
    logger.error(MESSAGE_BEFORE_APP_DEPLOYMENT);
    deployApp();
    logger.error(MESSAGE_AFTER_APP_DEPLOYMENT);

    assertAppLogsAreSentToFileAccordingToAppLogConfig();
    assertRootLogsAreSentToFileAccordingToAppLogConfig();
    assertRootLogsBeforeAppDeploymentAreNotSentToFileAccordingToAppLogConfig();

  }

  private void assertRootLogsAreSentToFileAccordingToAppLogConfig() {
    probeLogFileForMessage(MESSAGE_AFTER_APP_DEPLOYMENT);
  }

  private void assertRootLogsBeforeAppDeploymentAreNotSentToFileAccordingToAppLogConfig() {
    probeLogFileForNotExistingMessage(MESSAGE_BEFORE_APP_DEPLOYMENT);
  }

  private void assertAppLogsAreSentToFileAccordingToAppLogConfig() {
    probeLogFileForMessage("My logger is SLF4J");
    probeLogFileForMessage("My logger is LOG4J");
    probeLogFileForMessage("My logger is JCL");
  }

  private void probeLogFileForMessage(String expectedMessage) {
    File logFile = new File(muleServer.getLogsDir().toString() + getProperty("file.separator") + TEST_LOG_FILE);

    probe(() -> hasLine(containsString(expectedMessage)).matches(logFile),
          () -> format("Text '%s' not present in the logs", expectedMessage));
  }

  private void probeLogFileForNotExistingMessage(String expectedMessage) {
    File logFile = new File(muleServer.getLogsDir().toString() + getProperty("file.separator") + TEST_LOG_FILE);

    probe(() -> FileNotContainInLine.noLine(containsString(expectedMessage)).matches(logFile),
          () -> format("Text '%s' present in the logs", expectedMessage));
  }

  private void deployApp() throws URISyntaxException, IOException, MuleException {
    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder(LOGGING_APP_NAME).definedBy(APP_XML_FILE_NAME)
            .usingResource("log/logging-libs/resources/log4j2.xml",
                           "log4j2.xml")
            .dependingOn(new JarFileBuilder("loggerLibsClient",
                                            new CompilerUtils.JarCompiler()
                                                .compiling(new File(SingleAppModeLoggerReconfigurationOnMuleAppDeploymentTestCase.class
                                                    .getResource("/log/logging-libs/java/logging/LoggerLibsClient.java").toURI()))
                                                .compile("logger-libs-client.jar")));

    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), LOGGING_APP_NAME);
  }
}
