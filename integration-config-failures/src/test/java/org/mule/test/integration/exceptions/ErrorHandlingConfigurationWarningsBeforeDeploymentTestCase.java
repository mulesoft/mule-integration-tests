/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.AbstractConfigurationWarningsBeforeDeploymentTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;

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
               hasItem("'request' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  @Test
  @Issue("W-11802232")
  public void propertySourceErrorMappingsNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/property-error-mapping-source-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'request' has 'type' '${error.type}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
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
    ExtensionModel sockets = loadExtension(SocketsExtension.class, emptySet());
    ExtensionModel http = loadExtension(HttpConnector.class, singleton(sockets));

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(http);
    extensions.add(sockets);

    return extensions;
  }

}
