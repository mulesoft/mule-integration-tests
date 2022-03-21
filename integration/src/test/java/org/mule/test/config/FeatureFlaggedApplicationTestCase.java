/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.config.MuleRuntimeFeature.HANDLE_SPLITTER_EXCEPTION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;
import static org.mule.test.petstore.extension.PetStoreFeatures.LEGACY_FEATURE_ONE;
import static org.mule.test.petstore.extension.PetStoreFeatures.LEGACY_FEATURE_TWO;
import static org.mule.test.petstore.extension.PetStoreOperations.operationExecutionCounter;

import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

@RunnerDelegateTo(Parameterized.class)
@Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class FeatureFlaggedApplicationTestCase extends AbstractIntegrationTestCase {

  private static final String ECHO_MULE_CONTEXT = "echo-mule-context";
  private static final String ECHO_FEATURE_CONTEXT = "echo-feature-context";

  private static final String SPLITTER_EXCEPTION_FLOW = "splitter-exception";

  private static final String PAYLOAD = "bla";

  private final String flowName;
  private final String minMuleVersion;
  private final Consumer<CoreEvent> assertions;

  @Rule
  public SystemProperty systemProperty;

  @Parameterized.Parameters(name = "Feature {0} for minMuleVersion={2} and System Property={3}")
  public static Object[][] parameters() {
    return new Object[][] {
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.2.2", "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.2.2", "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.2.2", null, assertEcho(true)},

        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.3.0", "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.3.0", "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, "4.3.0", null, assertEcho(false)},

        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, null, "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, null, "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_ONE, ECHO_MULE_CONTEXT, null, null, assertEcho(false)},

        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.2.2", "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.2.2", "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.2.2", null, assertEcho(true)},

        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.3.0", "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.3.0", "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, "4.3.0", null, assertEcho(false)},

        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, null, "true", assertEcho(true)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, null, "false", assertEcho(false)},
        new Object[] {LEGACY_FEATURE_TWO, ECHO_FEATURE_CONTEXT, null, null, assertEcho(false)},

        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.2.2", "true", assertSplitterException(true)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.2.2", "false", assertSplitterException(false)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.2.2", null, assertSplitterException(false)},

        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.4.0", "true", assertSplitterException(true)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.4.0", "false", assertSplitterException(false)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, "4.4.0", null, assertSplitterException(true)},

        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, null, "true", assertSplitterException(true)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, null, "false", assertSplitterException(false)},
        new Object[] {HANDLE_SPLITTER_EXCEPTION, SPLITTER_EXCEPTION_FLOW, null, null, assertSplitterException(false)}
    };
  }


  public FeatureFlaggedApplicationTestCase(org.mule.runtime.api.config.Feature testingFeature, String flowName,
                                           String minMuleVersion, String systemPropertyValue,
                                           Consumer<CoreEvent> assertions) {
    this.flowName = flowName;
    this.minMuleVersion = minMuleVersion;
    this.assertions = assertions;
    if (systemPropertyValue != null && testingFeature.getOverridingSystemPropertyName().isPresent()) {
      this.systemProperty = new SystemProperty(testingFeature.getOverridingSystemPropertyName().get(), systemPropertyValue);
    }
  }

  @BeforeClass
  public static void registerFeatureFlags() {
    // just for load the static block on PetStoreOperations with the feature flagging registration logic
    operationExecutionCounter.get();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/config/feature-flagged-config.xml";
  }

  @Test
  public void getProperMessageDependingOnFeatureFlag() throws Exception {
    CoreEvent result = flowRunner(flowName).withPayload(PAYLOAD).run();
    assertThat(result.getMessage().getPayload(), is(notNullValue()));
    assertions.accept(result);
  }

  @Override
  protected MuleContext createMuleContext() throws Exception {
    MuleVersion muleVersion = null;
    if (minMuleVersion != null) {
      muleVersion = new MuleVersion(minMuleVersion);
    }
    return createMuleContext(this.getClass().getSimpleName() + "#" + name.getMethodName(), muleVersion);
  }

  private static Consumer<CoreEvent> assertEcho(boolean isLegacy) {
    return response -> {
      StringBuilder expected = new StringBuilder(PAYLOAD);
      if (isLegacy) {
        expected.append(" [old way]");
      }
      assertThat(response.getMessage().getPayload(), is(notNullValue()));
      assertThat(response.getMessage().getPayload().getValue(), is(expected.toString()));
    };
  }

  private static Consumer<CoreEvent> assertSplitterException(boolean expected) {
    return response -> assertThat(response.getMessage().getPayload().getValue(), is(expected));
  }

}
