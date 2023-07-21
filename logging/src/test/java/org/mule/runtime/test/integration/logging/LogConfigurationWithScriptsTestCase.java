/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.apache.logging.log4j.core.util.Constants.SCRIPT_LANGUAGES;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Checks log4j configurations with scripts
 */
@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Story(CONTEXT_FACTORY)
public class LogConfigurationWithScriptsTestCase extends AbstractFakeMuleServerTestCase {

  @ClassRule
  public static SystemProperty allowedScriptLanguagesSystemProperty =
      new SystemProperty(SCRIPT_LANGUAGES, "nashorn,js,javascript,ecmascript");

  private static final String APP_NAME = "app1";

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Test
  @Issue("W-12549148")
  public void scriptingSupport() throws Exception {
    muleServer.start();
    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder(APP_NAME).definedBy("log/script-config/mule-config.xml")
            .usingResource("log/script-config/log4j-config-scripting.xml", "log4j2-test.xml");
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    Application app = muleServer.findApplication(APP_NAME);
    assertThat(appHasAppender(app, "scripting"), is(true));

    File file = new File(muleServer.getLogsDir().toString() + "/success.log");

    String expectedMessage = "expected log message";
    String unexpectedMessage = "unwanted appender";
    probe(() -> hasLine(containsString(expectedMessage)).matches(file),
          () -> format("Text '%s' not present in the logs", expectedMessage));
    probe(() -> !hasLine(containsString(unexpectedMessage)).matches(file),
          () -> format("Text '%s' is present in the logs", unexpectedMessage));
  }

  private static boolean appHasAppender(Application app, String appenderName) {
    return getContext(app).getConfiguration().getLoggerConfig("").getAppenders().containsKey(appenderName);
  }

  private static LoggerContext getContext(Application app) {
    ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
    return (LoggerContext) LogManager.getContext(classLoader, false);
  }
}
