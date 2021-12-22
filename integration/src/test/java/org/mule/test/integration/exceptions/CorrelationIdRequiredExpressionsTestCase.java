/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CorrelationIdRequiredExpressionsTestCase extends AbstractConfigurationFailuresTestCase {

  /**
   * Configures the switch for {@link MuleRuntimeFeature#ENFORCE_REQUIRED_EXPRESSION_VALIDATION}.
   */
  @Parameterized.Parameters(name = "version: {0}")
  public static Collection<Object[]> featureFlags() {
    ExpectedException expected = none();
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage(containsString("A static value (''doge'') was given for parameter 'correlationIdGeneratorExpression' but it requires an expression"));
    ExpectedException expectedBefore = none();
    expectedBefore.expect(InitialisationException.class);
    expectedBefore
        .expectMessage(containsString("Invalid Correlation ID Generation expression: 'doge'"));

    return asList(new Object[][] {
        {"4.5.0", expected},
        {"4.4.0", expectedBefore}
    });
  }

  public MuleVersion minMuleVersion;

  @Rule
  public ExpectedException expectedException;

  public CorrelationIdRequiredExpressionsTestCase(String minMuleVersion, ExpectedException expectedException) {
    this.minMuleVersion = new MuleVersion(minMuleVersion);
    this.expectedException = expectedException;
  }

  @Test
  public void errorStaticValue() throws Exception {
    loadConfiguration("org/mule/test/config/correlation-id/static-generation.xml");
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(minMuleVersion);
  }
}
