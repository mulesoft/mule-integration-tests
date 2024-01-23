/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;

import org.apache.logging.log4j.core.selector.ContextSelector;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.log4j.internal.ApplicationReconfigurableLoggerContextSelector;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppModeLoggerTestCase extends AbstractFakeMuleServerTestCase {

  private ApplicationReconfigurableLoggerContextSelector contextSelector = new ApplicationReconfigurableLoggerContextSelector();
  @Rule
  public UseMuleLog4jContextFactory muleLogging =
      new UseMuleLog4jContextFactory(contextSelector);

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
  public void log4jLogsSuccessfully() throws Exception {
    startRuntimeWithApp();

    // All the logging should go to the file defined in the app log file.
    probeLogFileForMessage("My logger is LOG4J");
    probeLogFileForMessage("My logger is JUL");
    probeLogFileForMessage("My logger is LOG4J");
    probeLogFileForMessage("My logger is JCL");

    // The container notification must also be in the app log file.
    probeLogFileForMessage("Started app 'logging-app'");
  }

  private void probeLogFileForMessage(String expectedMessage) {
    File logFile = new File(muleServer.getLogsDir().toString() + "/test.log");

    probe(() -> hasLine(containsString(expectedMessage)).matches(logFile),
          () -> format("Text '%s' not present in the logs", expectedMessage));
  }

  private void startRuntimeWithApp() throws URISyntaxException, IOException, MuleException {
    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder("logging-app").definedBy("log/logging-libs/logging-libs-app.xml")
            .usingResource("log/logging-libs/resources/log4j2.xml",
                           "log4j2.xml")
            .dependingOn(new JarFileBuilder("loggerLibsClient",
                                            new CompilerUtils.JarCompiler()
                                                .compiling(new File(SingleAppModeLoggerTestCase.class
                                                    .getResource("/log/logging-libs/java/logging/LoggerLibsClient.java").toURI()))
                                                .compile("logger-libs-client.jar")));

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), "logging-app");
  }

  @Override
  protected Consumer<ClassLoader> getActionOnMuleArtifactDeployment() {
    return classLoader -> contextSelector.reconfigureAccordingToAppClassloader(classLoader);
  }
}
