/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer.discoverRuntimeExtensionModels;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.validation.ArtifactAstValidator;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.codejargon.feather.Feather;
import org.codejargon.feather.Provides;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Provides an example of how code running outside of the Mule Runtime may invoke the AST validations that require a base
 * registry.
 */
@Features({@Feature(ARTIFACT_AST), @Feature(EXPRESSION_LANGUAGE)})
@Story(DSL_VALIDATION_STORY)
public class ArtifactAstValidationsTestCase extends AbstractMuleContextTestCase {

  private static Set<ExtensionModel> runtimeExtensionModels;
  private DefaultExtensionManager extensionManager;

  @BeforeClass
  public static void beforeClass() throws Exception {
    runtimeExtensionModels = discoverRuntimeExtensionModels();
  }

  @Before
  public void before() throws Exception {
    extensionManager = new DefaultExtensionManager();
    muleContext.setExtensionManager(extensionManager);
    initialiseIfNeeded(extensionManager, muleContext);
  }

  @Test
  public void astValidationsWithBaseRegistryOutsideRuntime() throws ConfigurationException {
    ArtifactAst ast = buildArtifactAst("expression-language-illegal-syntax-dw-config.xml");

    List<ValidationResultItem> errors = doValidate(ast);

    assertThat(errors, hasSize(1));

    ValidationResultItem error = errors.get(0);
    assertThat(error.getMessage(), equalTo("Missing Expression"));
  }

  protected List<ValidationResultItem> doValidate(ArtifactAst ast) throws ConfigurationException {
    Feather feather = Feather.with(new BaseRegistryForValidationsModule());

    ArtifactAstValidator astValidator = validatorBuilder()
        .withValidationEnricher(feather::injectFields)
        .build();

    ValidationResult result = astValidator.validate(ast);

    final List<ValidationResultItem> errors = result.getItems().stream()
        .filter(v -> v.getValidation().getLevel().equals(ERROR))
        .collect(toList());

    return errors;
  }

  public class BaseRegistryForValidationsModule {

    @Provides
    @Singleton
    public ExpressionLanguage expressionLanguage() {
      return new WeaveDefaultExpressionLanguageFactoryService(null).create();
    }

  }

  protected ArtifactAst buildArtifactAst(final String configFile) {
    return AstXmlParser.builder()
        .withExtensionModels(muleContext.getExtensionManager().getExtensions())
        .withExtensionModels(runtimeExtensionModels)
        .withSchemaValidationsDisabled()
        .build()
        .parse(this.getClass().getClassLoader().getResource("ast/" + configFile));
  }
}
