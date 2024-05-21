/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.logging;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_XML_SDK_MDC_RESET_PROPERTY;
import static org.mule.runtime.extension.api.ExtensionConstants.XML_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.XML_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.boot.ExtensionLoaderUtils.getLoaderById;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.slf4j.MDC;

@RunnerDelegateTo(Parameterized.class)
public class LoggerMDCTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

  private static final String BASE_PATH_XML = "org/mule/test/integration/logging/";
  private static final String LOGGER_FLOW_XML = "logger-flow.xml";
  private static final String MODULE_TRACING_XML = "module-tracing.xml";
  private static final String TRACE_ID_NOT_EXPECTED_ERROR_MESSAGE = "traceId not expected outside XML SDK operation";

  @Parameterized.Parameters(name = "System Property: " + ENABLE_XML_SDK_MDC_RESET_PROPERTY + "={0}")
  public static List<Object[]> data() {
    ExpectedException expected = none();
    expected.expect(MuleException.class);
    expected.expectMessage(containsString(TRACE_ID_NOT_EXPECTED_ERROR_MESSAGE));

    return asList(new Object[][] {
        {"true", none()},
        {"false", expected}
    });
  }

  @Rule
  public ExpectedException expectedException;

  @Rule
  public SystemProperty xmlSdkMdcReset;

  public LoggerMDCTestCase(String enableXmlSdkReset, ExpectedException expectedException) {
    this.xmlSdkMdcReset = new SystemProperty(ENABLE_XML_SDK_MDC_RESET_PROPERTY, enableXmlSdkReset);
    this.expectedException = expectedException;
  }

  @Override
  protected String getConfigFile() {
    return BASE_PATH_XML + LOGGER_FLOW_XML;
  }

  @Test
  @Issue("W-15206528")
  public void testLoggerMDCContext() throws Exception {
    flowRunner("main-flow").run();
  }

  private String[] getModulePaths() {
    return new String[] {BASE_PATH_XML + MODULE_TRACING_XML};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        registerXmlExtensions(createExtensionManager(muleContext));
      }

      private void registerXmlExtensions(ExtensionManager extensionManager) {
        final Set<ExtensionModel> extensions = new HashSet<>();
        extensions.addAll(extensionManager.getExtensions());
        final ExtensionModelLoader loader = getLoaderById(XML_SDK_LOADER_ID);

        for (String modulePath : getModulePaths()) {
          ExtensionModel extensionModel = loader.loadExtensionModel(builder(getClass().getClassLoader(),
                                                                            getDefault(extensions))
                                                                                .addParameter(XML_SDK_RESOURCE_PROPERTY_NAME,
                                                                                              modulePath)
                                                                                .build());
          extensions.add(extensionModel);
          extensionManager.registerExtension(extensionModel);
        }
      }
    });
  }

  public static class LoggerMDCAssertProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) {
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      assertThat(TRACE_ID_NOT_EXPECTED_ERROR_MESSAGE, contextMap.get("trace-id"), is(nullValue()));
      assertThat(contextMap.get("main-trace-id"), is("traceId"));

      return event;
    }
  }
}
