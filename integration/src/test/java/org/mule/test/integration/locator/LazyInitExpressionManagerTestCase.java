/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_FUNCTIONS;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.Thread.currentThread;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(EXPRESSION_LANGUAGE)})
@Story(SUPPORT_FUNCTIONS)
public class LazyInitExpressionManagerTestCase extends AbstractIntegrationTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/lazy-expressions.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  private static ClassLoader recordedTccl;

  @Test
  @Issue("MULE-19468")
  @Description("Verify that lazyInit doesn't break the setting of the proper TCCL when calling DW.")
  public void callsWeaveFunctionFromReusableAppSuccess() throws Exception {
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation("callsWeaveFunctionFromReusableApp").build());

    flowRunner("callsWeaveFunctionFromReusableApp").run();
    assertThat(recordedTccl, sameInstance(muleContext.getExecutionClassLoader()));
  }

  public static Object recordTccl(Object payload) {
    recordedTccl = currentThread().getContextClassLoader();
    return payload;
  }
}
