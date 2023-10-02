/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
public class ErrorHandlerLoggingTestCase extends AbstractFakeMuleServerTestCase {

  public static final String CONFIG_FILE = "log/error-handler-logging.xml";
  public static final String APP_NAME = "scheduler-and-error-handler-log-exception-false";
  public static final String APP_LOG = format("/mule-app-%s.log", APP_NAME);
  public static final String LOG_MESSAGE = "An error occurred";

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Test
  @Issue("W-13881167")
  public void schedulerAndErrorHandlerWithLogExceptionSetFalseDoesNotLog() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage();
  }

  private void startRuntimeWithApp() throws URISyntaxException, IOException, MuleException {
    final ApplicationFileBuilder loggingAppFileBuilder = new ApplicationFileBuilder(APP_NAME).definedBy(CONFIG_FILE);
    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
  }

  private void probeLogFileForMessage() {
    File logFile = new File(muleServer.getLogsDir().toString() + APP_LOG);
    probe(() -> !hasLine(containsString(LOG_MESSAGE)).matches(logFile),
          () -> format("The text '%s' is not present in the logs.", LOG_MESSAGE));
  }
}
