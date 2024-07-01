/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.AbstractConfigurationWarningsBeforeDeploymentTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Story(DSL_VALIDATION_STORY)
public class StereotypeConfigurationWarningsBeforeDeploymentTestCase
    extends AbstractConfigurationWarningsBeforeDeploymentTestCase {

  @Test
  @Issue("W-11802232")
  public void failureOnInvalidImport() throws Exception {
    loadConfiguration("org/mule/config/spring/parsers/dsl-validation-stereotype-config.xml");

    assertThat(getWarningMessages(),
               hasItem("'http:listener' has 'config-ref' '${http.listener.name}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
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
