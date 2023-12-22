/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.MAX_CONCURRENCY;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.CoreMatchers.containsString;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Issue("W-14642249")
@Feature(CORE_COMPONENTS)
@Stories({@Story(DSL_VALIDATION_STORY), @Story(MAX_CONCURRENCY)})
public class MaxConcurrencyFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void flowNegativeMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/flow-max-concurrency-negative.xml:8]: "
            + "Parameter 'maxConcurrency' in element <flow> value '-1' is not within expected range."));
    loadConfiguration("org/mule/processors/flow-max-concurrency-negative.xml");
  }

  @Test
  public void flowZeroMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/flow-max-concurrency-zero.xml:8]: "
            + "Parameter 'maxConcurrency' in element <flow> value '0' is not within expected range."));
    loadConfiguration("org/mule/processors/flow-max-concurrency-zero.xml");
  }

  @Test
  public void asyncNegativeMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/async-max-concurrency-negative.xml:9]: "
            + "Parameter 'maxConcurrency' in element <async> value '-1' is not within expected range."));
    loadConfiguration("org/mule/processors/async-max-concurrency-negative.xml");
  }

  @Test
  public void asyncZeroMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/async-max-concurrency-zero.xml:9]: "
            + "Parameter 'maxConcurrency' in element <async> value '0' is not within expected range."));
    loadConfiguration("org/mule/processors/async-max-concurrency-zero.xml");
  }

  @Test
  public void scatterGatherNegativeMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/scatter-gather-max-concurrency-negative.xml:9]: "
            + "Parameter 'maxConcurrency' in element <scatter-gather> value '-1' is not within expected range."));
    loadConfiguration("org/mule/processors/scatter-gather-max-concurrency-negative.xml");
  }

  @Test
  public void scatterGatherZeroMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/scatter-gather-max-concurrency-zero.xml:9]: "
            + "Parameter 'maxConcurrency' in element <scatter-gather> value '0' is not within expected range."));
    loadConfiguration("org/mule/processors/scatter-gather-max-concurrency-zero.xml");
  }

  @Test
  public void parallelforeachNegativeMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/parallel-foreach-max-concurrency-negative.xml:9]: "
            + "Parameter 'maxConcurrency' in element <parallel-foreach> value '-1' is not within expected range."));
    loadConfiguration("org/mule/processors/parallel-foreach-max-concurrency-negative.xml");
  }

  @Test
  public void parallelforeachZeroMaxConcurrency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/parallel-foreach-max-concurrency-zero.xml:9]: "
            + "Parameter 'maxConcurrency' in element <parallel-foreach> value '0' is not within expected range."));
    loadConfiguration("org/mule/processors/parallel-foreach-max-concurrency-zero.xml");
  }

}
