/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Collections.emptySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;

import org.mule.functional.junit4.AbstractConfigurationWarningsBeforeDeploymentTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(DSL_VALIDATION_STORY)
public class ErrorHandlingConfigurationWarningsBeforeDeploymentTestCase
    extends AbstractConfigurationWarningsBeforeDeploymentTestCase {

  @Test
  @Issue("W-11802232")
  public void raisesErrorPropertyErrorTypeNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/raise-error-property-type-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'raise-error' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  @Test
  @Issue("W-11802232")
  public void propertyTargetErrorMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/property-error-mapping-target-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'cure-cancer' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  @Test
  @Issue("W-11802232")
  public void propertySourceErrorMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/property-error-mapping-source-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'cure-cancer' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  @Test
  @Issue("W-11802232")
  public void propertyOnErrorNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/property-on-error-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'on-error-propagate' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel heisenberg = loadExtension(HeisenbergExtension.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(heisenberg);

    return extensions;
  }

  @Override
  public boolean ignoreParamsWithProperties() {
    return true;
  }

}
