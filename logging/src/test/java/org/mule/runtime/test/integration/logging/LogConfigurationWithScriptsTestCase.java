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
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.apache.commons.lang3.JavaVersion.JAVA_11;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.apache.logging.log4j.core.util.Constants.SCRIPT_LANGUAGES;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

/**
 * Checks log4j configurations with scripts
 */
@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Story(CONTEXT_FACTORY)
public class LogConfigurationWithScriptsTestCase extends AbstractFakeMuleServerTestCase {

  @ClassRule
  public static SystemProperty allowedScriptLanguagesSystemProperty =
      new SystemProperty(SCRIPT_LANGUAGES, "javascript");

  private static final String APP_NAME = "app1";

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Test
  @Issue("W-12549148")
  public void scriptingSupport() throws Exception {
    muleServer.start();
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME)
        .definedBy("log/script-config/mule-config.xml")
        .usingResource("log/script-config/log4j-config-scripting.xml", "log4j2-test.xml");
    if (isJavaVersionAtLeast(JAVA_11)) {
      applicationFileBuilder =
          applicationFileBuilder
              .dependingOn(new JarFileBuilder("ibm-icu4j",
                                              new File(getProperty("ibmIcu4jJarLoc"))))
              .dependingOn(new JarFileBuilder("graal-sdk",
                                              new File(getProperty("graalVmSdkJarLoc"))))
              .dependingOn(new JarFileBuilder("truffle-api",
                                              new File(getProperty("graalVmTruffleJarLoc"))))
              .dependingOn(new JarFileBuilder("js",
                                              new File(getProperty("graalVmJsJarLoc"))))
              .dependingOn(new JarFileBuilder("js-scriptengine",
                                              new File(getProperty("graalVmJsScriptEngineJarLoc"))));
    }

    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    Application app = muleServer.findApplication(APP_NAME);
    assertThat(appLogAppenders(app), hasKey("scripting"));

    File file = new File(muleServer.getLogsDir().toString() + "/success.log");

    String expectedMessage = "expected log message";
    String unexpectedMessage = "unwanted appender";
    probe(() -> hasLine(containsString(expectedMessage)).matches(file),
          () -> format("Text '%s' not present in the logs", expectedMessage));
    probe(() -> !hasLine(containsString(unexpectedMessage)).matches(file),
          () -> format("Text '%s' is present in the logs", unexpectedMessage));
  }

  private static Map<String, Appender> appLogAppenders(Application app) {
    return getContext(app).getConfiguration().getLoggerConfig("").getAppenders();
  }

  private static LoggerContext getContext(Application app) {
    ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
    return (LoggerContext) LogManager.getContext(classLoader, false);
  }

}
