/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;

public class ConfigurationProcessingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty frequency = new SystemProperty("frequency", "1000");

  @Inject
  @Named("simpleFlow")
  private Flow simpleFlow;

  @Inject
  @Named("complexFlow")
  private Flow complexFlow;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/dsl/parsing-test-config.xml";
  }

  @Test
  public void simpleFlowConfiguration() throws Exception {
    assertThat(simpleFlow.getProcessors(), notNullValue());
    assertThat(simpleFlow.getProcessors().size(), is(1));
  }

  @Test
  public void complexFlowConfiguration() throws Exception {
    assertThat(complexFlow.getSource(), notNullValue());
    assertThat(complexFlow.getProcessors(), notNullValue());
    assertThat(complexFlow.getProcessors().size(), is(4));
  }
}
