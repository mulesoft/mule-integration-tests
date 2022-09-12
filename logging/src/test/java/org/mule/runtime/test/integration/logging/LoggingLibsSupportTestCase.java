/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.LoggerStory.LOGGER;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.junit4.FlakyTest;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.slf4j.bridge.SLF4JBridgeHandler;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTEGRATIONS_TESTS)
@Story(LOGGER)
@RunWith(FlakinessDetectorTestRunner.class)
@FlakyTest
public class LoggingLibsSupportTestCase extends AbstractFakeMuleServerTestCase {

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    SLF4JBridgeHandler.uninstall();
  }

  @Test
  public void slf4jLibraryLogsSuccessfully() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage("My logger is SLF4J");
  }

  @Test
  public void log4jLibraryLogsSuccessfully() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage("My logger is LOG4J");
  }

  @Test
  @Ignore("W-11730386")
  public void jclLibraryLogsSuccessfully() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage("My logger is JCL");
  }

  @Test
  public void julLibraryLogsSuccessfully() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage("My logger is JUL");
  }

  private void probeLogFileForMessage(String expectedMessage) {
    File logFile = new File(muleServer.getLogsDir().toString() + "/mule-app-logging-app.log");

    probe(() -> hasLine(containsString(expectedMessage)).matches(logFile),
          () -> format("Text '%s' not present in the logs", expectedMessage));
  }

  private void startRuntimeWithApp() throws URISyntaxException, IOException, MuleException, MalformedURLException {
    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder("logging-app").definedBy("log/logging-libs/logging-libs-app.xml")
            .dependingOn(new JarFileBuilder("loggerLibsClient",
                                            new CompilerUtils.JarCompiler()
                                                .compiling(new File(LoggingLibsSupportTestCase.class
                                                    .getResource("/log/logging-libs/java/logging/LoggerLibsClient.java").toURI()))
                                                .compile("logger-libs-client.jar")));

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), "logging-app");
  }
}
