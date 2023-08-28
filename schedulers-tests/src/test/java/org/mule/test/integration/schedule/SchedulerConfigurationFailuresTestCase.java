/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.containsString;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SCHEDULER)
@Story(DSL_VALIDATION_STORY)
public class SchedulerConfigurationFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void coreSchedulerNoSchedulingStrategy() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(containsString("The scheduling strategy has not been configured."));
    loadConfiguration("org/mule/test/integration/invalid/core-scheduler-no-scheduling-strategy.xml");
  }

  @Test
  public void petStoreSchedulerNoSchedulingStrategy() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(containsString("The scheduling strategy has not been configured."));
    loadConfiguration("org/mule/test/integration/invalid/pet-store-scheduler-no-scheduling-strategy.xml");
  }

  @Override
  protected boolean disableXmlValidations() {
    return true;
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel petStore = loadExtension(PetStoreConnector.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(petStore);

    return extensions;
  }
}
