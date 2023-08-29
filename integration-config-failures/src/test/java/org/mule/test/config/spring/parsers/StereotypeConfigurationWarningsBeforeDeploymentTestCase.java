/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.fail;

import org.mule.extension.http.internal.temporary.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.dsl.api.ConfigResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Story(DSL_VALIDATION_STORY)
public class StereotypeConfigurationWarningsBeforeDeploymentTestCase extends AbstractConfigurationFailuresTestCase {

  private List<String> warningMessages;

  @Test
  @Issue("W-11802232")
  public void failureOnInvalidImport() throws Exception {
    loadConfiguration("org/mule/config/spring/parsers/dsl-validation-stereotype-config.xml");

    assertThat(warningMessages,
               hasItem("'http:listener' has 'config-ref' '${http.listener.name}' which is resolved with a property and may cause the artifact to have different behavior on different environments."));
  }

  private AstXmlParser getParser(Set<ExtensionModel> extensions) {
    Builder builder = AstXmlParser.builder()
        .withExtensionModels(extensions)
        .withArtifactType(APPLICATION)
        .withParentArtifact(emptyArtifact());

    return builder.build();
  }

  @Override
  protected void loadConfiguration(String configuration) throws MuleException, InterruptedException {
    ArtifactAst ast;
    try {
      ast = getParser(new HashSet<>(getRequiredExtensions())).parse(new ConfigResource(configuration));
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }

    ValidationResult validationResult = validatorBuilder()
        .ignoreParamsWithProperties(true)
        .build()
        .validate(ast);

    validationResult.getItems()
        .stream()
        .filter(vri -> vri.getValidation().getLevel().equals(ERROR))
        .forEach(vri -> fail(vri.getMessage()));

    this.warningMessages = validationResult.getItems()
        .stream()
        .filter(vri -> vri.getValidation().getLevel().equals(WARN))
        .map(v -> v.getMessage())
        .collect(toList());
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
