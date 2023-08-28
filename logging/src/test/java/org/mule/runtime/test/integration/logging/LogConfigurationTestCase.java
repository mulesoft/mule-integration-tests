/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SIMPLE_LOG;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.CONTEXT_FACTORY;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.LOGGING_LIBS_SUPPORT;
import static org.mule.test.infrastructure.FileContainsInLine.hasLine;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

/**
 * Checks log4j configuration for application and domains
 */
@Features({@Feature(INTEGRATIONS_TESTS), @Feature(LOGGING)})
@Stories({@Story(LOGGING_LIBS_SUPPORT), @Story(CONTEXT_FACTORY)})
public class LogConfigurationTestCase extends AbstractFakeMuleServerTestCase {

  public static final String APP_NAME = "app1";
  public static final String DOMAIN_NAME = "domain";

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @ClassRule
  public static SystemProperty customAppDebugLogSystemProperty =
      new SystemProperty("customAppDebugLogPropertyValue", "Custom Log App log");

  @ClassRule
  public static SystemProperty notCustomAppDebugLogSystemProperty =
      new SystemProperty("notCustomAppDebugLogPropertyValue", "Not Custom Log App log");

  @Override
  public void setUp() throws Exception {
    super.setUp();

    // here we're trying to test log separation so we need to
    // disable this default property of the fake mule server
    // in order to test that
    System.clearProperty(MULE_SIMPLE_LOG);
  }

  @Test
  public void defaultAppLoggingConfigurationOnlyLogsOnApplicationLogFile() throws Exception {
    muleServer.start();
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME).definedBy("log/empty-config.xml");
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    ensureOnlyDefaultAppender();
  }

  @Test
  public void defaultAppInDomainLoggingConfigurationOnlyLogsOnApplicationLogFile() throws Exception {
    muleServer.start();
    DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(DOMAIN_NAME).definedBy("log/empty-domain/empty-domain-config.xml");
    muleServer.deployDomainFile(domainFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder(APP_NAME).dependingOn(domainFileBuilder).definedBy("log/empty-config.xml");
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    ensureOnlyDefaultAppender();
  }

  @Test
  public void honorLog4jConfigFileForApp() throws Exception {
    muleServer.start();
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME).definedBy("log/empty-config.xml")
        .usingResource("log/log4j-config.xml", "log4j2-test.xml");
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    ensureArtifactAppender("consoleForApp", ConsoleAppender.class);
  }

  @Test
  public void honorLog4jConfigFileWithNoAppenderForApp() throws Exception {
    muleServer.start();
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME).definedBy("log/empty-config.xml")
        .usingResource("log/log4j-config-no-appender.xml", "log4j2-test.xml");
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    ensureOnlyDefaultAppender();
  }

  @Test
  public void honorLog4jConfigFileForAppInDomain() throws Exception {
    muleServer.start();

    DomainFileBuilder domainFileBuilder = new DomainFileBuilder(DOMAIN_NAME)
        .definedBy("log/empty-domain-with-log4j/empty-domain-config.xml")
        .containingResource("log/empty-domain-with-log4j/log4j2-test.xml", "log4j2-test.xml");
    muleServer.deployDomainFile(domainFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME)
        .definedBy("log/empty-config.xml").dependingOn(domainFileBuilder);
    muleServer.deploy(applicationFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);
    ensureArtifactAppender("ConsoleForDomain", ConsoleAppender.class);
  }

  @Test
  @Issue("W-11090843")
  public void honorLog4jConfigFileForTwoAppsWithDifferentConfiguration() throws Exception {
    String customLogAppName = "custom-log-app";
    String notCustomLogAppName = "not-custom-log-app";

    ApplicationFileBuilder customLogAppBuilder = new ApplicationFileBuilder(customLogAppName)
        .definedBy("log/custom-config/mule-config.xml")
        .containingResource("log/custom-config/log4j2.xml", "log4j2.xml");
    ApplicationFileBuilder notCustomLogAppFileBuilder = new ApplicationFileBuilder(notCustomLogAppName)
        .definedBy("log/not-custom-config/mule-config.xml");

    muleServer.addAppArchive(customLogAppBuilder.getArtifactFile().toURI().toURL());
    muleServer.addAppArchive(notCustomLogAppFileBuilder.getArtifactFile().toURI().toURL());

    muleServer.start();

    File customAppLogFile =
        new File(muleServer.getLogsDir().toString() + "/mule-app-" + customLogAppName + "-1.0.0-mule-application.log");
    probe(() -> hasLine(containsString(customAppDebugLogSystemProperty.getValue())).matches(customAppLogFile),
          () -> format("Text '%s' not present in the logs", customAppDebugLogSystemProperty.getValue()));
    probe(() -> !hasLine(containsString(notCustomAppDebugLogSystemProperty.getValue())).matches(customAppLogFile),
          () -> format("Text '%s' present in the logs", notCustomAppDebugLogSystemProperty.getValue()));

    File notCustomAppLogFile =
        new File(muleServer.getLogsDir().toString() + "/mule-app-" + notCustomLogAppName + "-1.0.0-mule-application.log");
    probe(() -> !hasLine(containsString(customAppDebugLogSystemProperty.getValue())).matches(notCustomAppLogFile),
          () -> format("Text '%s' not present in the logs", customAppDebugLogSystemProperty.getValue()));
    probe(() -> !hasLine(containsString(notCustomAppDebugLogSystemProperty.getValue())).matches(notCustomAppLogFile),
          () -> format("Text '%s' not present in the logs", notCustomAppDebugLogSystemProperty.getValue()));
  }

  private void ensureOnlyDefaultAppender() throws Exception {
    assertThat(1, equalTo(appendersCount(APP_NAME)));
    assertThat(1, equalTo(selectByClass(APP_NAME, RollingFileAppender.class).size()));

    RollingFileAppender fileAppender = (RollingFileAppender) selectByClass(APP_NAME, RollingFileAppender.class).get(0);
    assertThat("defaultFileAppender", equalTo(fileAppender.getName()));
    assertThat(fileAppender.getFileName(), containsString(format("mule-app-%s.log", APP_NAME)));
  }

  private void ensureArtifactAppender(final String appenderName, final Class<? extends Appender> appenderClass) throws Exception {
    withAppClassLoader(APP_NAME, () -> {
      Logger logger = getRootLoggerForApp(APP_NAME);
      assertThat(Level.WARN, equalTo(logger.getLevel()));
      assertThat(true, equalTo(loggerHasAppender(APP_NAME, logger, appenderName)));


      assertThat(1, equalTo(appendersCount(APP_NAME)));
      assertThat(1, equalTo(selectByClass(APP_NAME, appenderClass).size()));
      assertThat(appenderName, equalTo(selectByClass(APP_NAME, appenderClass).get(0).getName()));

      return null;
    });
  }

  private boolean loggerHasAppender(String appName, Logger logger, String appenderName) throws Exception {
    return getContext(appName).getConfiguration().getLoggerConfig(logger.getName()).getAppenders().containsKey(appenderName);
  }

  private Logger getRootLoggerForApp(String appName) throws Exception {
    return getContext(appName).getLogger("");
  }

  private LoggerContext getContext(final String appName) throws Exception {
    return withAppClassLoader(appName, () -> {
      Application app = muleServer.findApplication(appName);
      ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
      return (LoggerContext) LogManager.getContext(classLoader, false);
    });
  }

  private <T> T withAppClassLoader(String appName, Callable<T> closure) throws Exception {
    Application app = muleServer.findApplication(appName);
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader classLoader = app.getArtifactClassLoader().getClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      return closure.call();
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  private List<Appender> selectByClass(String appName, Class<?> appenderClass) throws Exception {
    LoggerContext context = getContext(appName);
    List<Appender> filteredAppenders = new LinkedList<>();
    for (Appender appender : context.getConfiguration().getAppenders().values()) {
      if (appenderClass.isAssignableFrom(appender.getClass())) {
        filteredAppenders.add(appender);
      }
    }

    return filteredAppenders;
  }

  private int appendersCount(String appName) throws Exception {
    return selectByClass(appName, Appender.class).size();
  }
}
