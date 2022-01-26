/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.LoggerStory.LOGGER;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;


@Feature(INTEGRATIONS_TESTS)
@Story(LOGGER)
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
  public void eachLoggingLibraryShouldLogSuccessfully() throws Exception {
    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder("logging-app").definedBy("log/logging-libs/logging-libs-app.xml")
            .dependingOn(new JarFileBuilder("loggerLibsClient",
                                            new CompilerUtils.JarCompiler()
                                                .compiling(new File(LoggingLibsSupportTestCase.class
                                                    .getResource("/log/logging-libs/java/logging/LoggerLibsClient.java").toURI()))
                                                .compile("logger-libs-client.jar")));

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), "logging-app");

    File file = new File(muleServer.getLogsDir().toString() + "/mule-app-logging-app.log");

    probe(() -> hasLine(containsString("My logger is SLF4J")).matches(file),
          () -> format("Text '%s' not present in the logs", "My logger is SLF4J"));
    probe(() -> hasLine(containsString("My logger is LOG4J")).matches(file),
          () -> format("Text '%s' not present in the logs", "My logger is LOG4J"));
    probe(() -> hasLine(containsString("My logger is JCL")).matches(file),
          () -> format("Text '%s' not present in the logs", "My logger is JCL"));
    probe(() -> hasLine(containsString("My logger is JUL")).matches(file),
          () -> format("Text '%s' not present in the logs", "My logger is JUL"));
  }
}
