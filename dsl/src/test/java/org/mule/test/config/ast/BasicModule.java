/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.ast;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_EXPRESSION_VALIDATION;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.ast.api.validation.ArtifactAstValidatorBuilder;
import org.mule.runtime.ast.api.validation.ValidationsProvider;
import org.mule.runtime.ast.internal.validation.DefaultValidatorBuilder;
import org.mule.runtime.config.internal.validation.CoreValidationsProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.runtime.core.internal.transformer.simple.StringToBoolean;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.runtime.feature.internal.config.DefaultFeatureFlaggingService;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * This class will serve as an example of how to inject fields for AST validators in a Java 17 compliant way using some DI
 * framework, Guice in this example. Earlier we were using feather and that does not work with Java 17. The way injection done
 * here works both with Java 8 and 17
 */
class BasicModule extends AbstractModule {

  private Map<org.mule.runtime.api.config.Feature, Boolean> featureBooleanMap =
      singletonMap(ENFORCE_EXPRESSION_VALIDATION, Boolean.valueOf(true));

  @Override
  protected void configure() {
    bind(ArtifactAstValidatorBuilder.class).to(DefaultValidatorBuilder.class);
    bind(ExtendedExpressionManager.class).to(DefaultExpressionManager.class);
    bind(ValidationsProvider.class).to(CoreValidationsProvider.class);
    bind(MuleContext.class).to(DefaultMuleContext.class);
    bind(TransformersRegistry.class).to(DefaultTransformersRegistry.class);
    bind(Object.class).to(String.class);
    bind(Transformer.class).to(StringToBoolean.class);
  }

  @Provides
  @Singleton
  public ExpressionLanguage provideExpressionLanguage() {
    return new WeaveDefaultExpressionLanguageFactoryService(null).create();
  }

  @Provides
  @Singleton
  public FeatureFlaggingService provideFeatureFlaggingService() {
    return new DefaultFeatureFlaggingService("abcd", featureBooleanMap);
  }

  @Provides
  @Singleton
  @Named("_compatibilityPluginInstalled")
  public Optional<Object> provideOptionalCompatibilityPluginInstalled(Object object) {
    return of(object);
  }

  @Provides
  @Singleton
  public Optional<FeatureFlaggingService> provideOptionalFeatureFlaggingService(FeatureFlaggingService featureFlaggingService) {
    return of(featureFlaggingService);
  }

  @Provides
  @Singleton
  public Collection<Transformer> provideCollectionTransformer() {
    return emptyList();
  }

  @Provides
  @Singleton
  public List<TransformerResolver> provideListTransformerResolver() {
    return emptyList();
  }
}

