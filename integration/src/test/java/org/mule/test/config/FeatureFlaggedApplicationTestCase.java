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
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;
import static org.mule.test.petstore.extension.PetStoreOperations.operationExecutionCounter;

import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

@RunnerDelegateTo(Parameterized.class)
@Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class FeatureFlaggedApplicationTestCase extends AbstractIntegrationTestCase {

  private static final String FLOW_NAME = "echo";

  private static final String PAYLOAD = "bla";

  private final String minMuleVersion;

  private final boolean isLegacy;

  @Parameterized.Parameters(name = "With minMuleVersion {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        new Object[] {true, "4.2.2"},
        new Object[] {false, "4.3.0"},
        new Object[] {false, null}
    };
  }

  public FeatureFlaggedApplicationTestCase(boolean isLegacy, String minMuleVersion) {
    this.isLegacy = isLegacy;
    this.minMuleVersion = minMuleVersion;
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
    CoreEvent responseEvent = flowRunner(FLOW_NAME).withPayload(PAYLOAD).run();
    StringBuilder expected = new StringBuilder(PAYLOAD);
    if (isLegacy) {
      expected.append(" [old way]");
    }
    assertThat(responseEvent.getMessage().getPayload(), is(notNullValue()));
    assertThat(responseEvent.getMessage().getPayload().getValue(), is(expected.toString()));
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {

      }

      @Override
      public void configure(MuleContext muleContext) {
        if (minMuleVersion != null) {
          ((DefaultMuleConfiguration) muleContext.getConfiguration()).setMinMuleVersion(new MuleVersion(minMuleVersion));
        }
      }
    });
    super.addBuilders(builders);
  }

}
