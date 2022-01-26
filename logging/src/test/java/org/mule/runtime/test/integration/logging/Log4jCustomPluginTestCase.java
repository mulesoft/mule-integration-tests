/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.LoggerStory.LOGGER;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(INTEGRATIONS_TESTS)
@Story(LOGGER)
public class Log4jCustomPluginTestCase extends AbstractFakeMuleServerTestCase {

  public static final String APP_NAME = "app1";

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Test
  public void useLog4jConfigFilePluginSuccessfully() throws Exception {
    File customLogInterceptor =
        new CompilerUtils.SingleClassCompiler()
            .compile(new File(requireNonNull(LogConfigurationTestCase.class
                .getResource("/log/log4j-plugin/java/com/mycompany/log4j/logger/CustomLogInterceptor.java")).toURI()));

    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder(APP_NAME).definedBy("log/log4j-plugin/mule/log4j-plugin-app.xml")
            .containingClass(customLogInterceptor, "com/mycompany/log4j/logger/CustomLogInterceptor.class")
            .usingResource("log/log4j-plugin/resources/log4j2.xml", "log4j2.xml");

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);

    File file = new File(muleServer.getLogsDir().toString() + "/log4j-plugin.log");

    String expectedMessage = "I have intercepted your message :)";
    String unexpectedMessage = "This log message should be intercepted...";
    probe(() -> hasLine(containsString(expectedMessage)).matches(file),
          () -> format("Text '%s' not present in the logs", expectedMessage));
    probe(() -> !hasLine(containsString(unexpectedMessage)).matches(file),
          () -> format("Text '%s' is present in the logs", unexpectedMessage));
  }
}
