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
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.LOGGING_LIBS_SUPPORT;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.test.integration.logging.util.UseMuleLog4jContextFactory;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Issue;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Story(LOGGING_LIBS_SUPPORT)
@Issue("W-11730386")
public class JclLoggingSupportTestCase extends AbstractFakeMuleServerTestCase {

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Test
  public void jclLibraryLogsSuccessfully() throws Exception {
    startRuntimeWithApp();
    probeLogFileForMessage("My logger is JCL");
  }

  private void probeLogFileForMessage(String expectedMessage) {
    File logFile = new File(muleServer.getLogsDir().toString() + "/mule-app-logging-app.log");

    probe(() -> hasLine(containsString(expectedMessage)).matches(logFile),
          () -> format("Text '%s' not present in the logs", expectedMessage));
  }

  private void startRuntimeWithApp() throws URISyntaxException, IOException, MuleException {
    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder("logging-app").definedBy("log/logging-libs/jcl-logging-app.xml")
            .dependingOn(new JarFileBuilder("JclLoggerClient",
                                            new CompilerUtils.JarCompiler()
                                                .compiling(new File(JclLoggingSupportTestCase.class
                                                    .getResource("/log/logging-libs/java/logging/JclLoggerClient.java").toURI()))
                                                .compile("jcl-logger-client.jar")));

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), "logging-app");
  }
}
