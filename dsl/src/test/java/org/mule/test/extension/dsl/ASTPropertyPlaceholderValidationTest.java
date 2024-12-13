/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ComponentElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConfigurationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConnectionElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConstructElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.OperationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterizedElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.RouteElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.SourceElementDeclarer;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.validation.ArtifactAstValidator;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.QNameModelProperty;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class ASTPropertyPlaceholderValidationTest extends AbstractElementModelTestCase {

  private static final Set<String> excludedValidatorClassNames =
      ImmutableSet.of("RequiredParametersPresent", "PollingSourceHasSchedulingStrategy", "ScatterGatherRoutes");
  private static Set<ExtensionModel> runtimeExtensionModels;
  private ArtifactDeclarationXmlSerializer serializer;
  private DslSyntaxResolver dslResolver;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Override
  @Before
  public void setup() {
    runtimeExtensionModels = muleContext.getExtensionManager().getExtensions();
    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(runtimeExtensionModels)
        .add(MuleExtensionModelProvider.getExtensionModel()).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
    serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
  }

  @Test
  public void propertyPlaceholderValidation() {
    String serializationResult = serializer.serialize(generateArtifactFromExtensionModels());
    ArtifactAst ast = buildArtifactAst(serializationResult);
    List<ValidationResultItem> errors = doValidate(ast);

    assertThat(errors, iterableWithSize(0));
  }

  protected List<ValidationResultItem> doValidate(ArtifactAst ast) {
    ArtifactAstValidator astValidator = validatorBuilder()
        .ignoreParamsWithProperties(true)
        .withValidationsFilter(v -> !excludedValidatorClassNames.contains(v.getClass().getSimpleName()))
        .build();

    ValidationResult result = astValidator.validate(ast);

    final List<ValidationResultItem> errors = result.getItems().stream()
        .filter(v -> v.getValidation().getLevel().equals(ERROR))
        .collect(toList());

    return errors;
  }

  protected ArtifactAst buildArtifactAst(final String configFile) {
    return AstXmlParser.builder()
        .withExtensionModels(muleContext.getExtensionManager().getExtensions())
        .withExtensionModels(runtimeExtensionModels)
        .withSchemaValidationsDisabled()
        .build()
        .parse("artifact", IOUtils.toInputStream(configFile, StandardCharsets.UTF_8));
  }

  private ArtifactDeclaration generateArtifactFromExtensionModels() {
    ExtensionModel extensionModel = MuleExtensionModelProvider.getExtensionModel();
    ElementDeclarer core = ElementDeclarer.forExtension(extensionModel.getName());
    ArtifactDeclarer artifactDeclarer = newArtifact();
    dslResolver = DslSyntaxResolver.getDefault(extensionModel, dslContext);

    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel configModel) {
        if (configModel.getConnectionProviders().isEmpty()) {
          // only one config
          ConfigurationElementDeclarer declarer = core.newConfiguration(configModel.getName()).withRefName("config");
          populateParameterized(configModel, declarer);
          artifactDeclarer.withGlobalElement(declarer.getDeclaration());

        } else {
          // one config per connection
          configModel.getConnectionProviders()
              .forEach(connection -> {
                ConnectionElementDeclarer connectionDeclarer = core.newConnection(connection.getName());
                populateParameterized(connection, connectionDeclarer);

                ConfigurationElementDeclarer configurationDeclarer = core.newConfiguration(configModel.getName())
                    .withRefName("config" + connection.getName())
                    .withConnection(connectionDeclarer.getDeclaration());

                populateParameterized(configModel, configurationDeclarer);
                artifactDeclarer.withGlobalElement(configurationDeclarer.getDeclaration());
              });
        }
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        ConstructElementDeclarer declarer = core.newConstruct(model.getName());
        populateParameterized(model, declarer);

        populateNested(model, declarer);

        if (model.allowsTopLevelDeclaration()) {
          artifactDeclarer.withGlobalElement(declarer.withRefName("global-" + model.getName()).getDeclaration());
        } else {
          artifactDeclarer.withGlobalElement(core.newConstruct("flow")
              .withRefName("flowFor-" + model.getName())
              .withComponent(declarer.getDeclaration())
              .getDeclaration());
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (model.getName().equals("flowRef")) {
          return;
        }
        OperationElementDeclarer declarer = core.newOperation(model.getName());
        populateParameterized(model, declarer);
        populateNested(model, declarer);
        artifactDeclarer.withGlobalElement(core.newConstruct("flow")
            .withRefName("flowFor-" + model.getName())
            .withComponent(declarer.getDeclaration())
            .getDeclaration());
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        SourceElementDeclarer declarer = core.newSource(model.getName());
        populateParameterized(model, declarer);
        artifactDeclarer.withGlobalElement(core.newConstruct("flow")
            .withRefName("flowFor-" + model.getName())
            .withComponent(declarer.getDeclaration())
            .getDeclaration());
      }

      private void populateNested(ComponentModel model, ComponentElementDeclarer declarer) {
        model.getNestedComponents().forEach(nestedPlaceholder -> {
          nestedPlaceholder.accept(new NestableElementModelVisitor() {

            @Override
            public void visit(NestedComponentModel component) {
              declarer.withComponent(core.newOperation("logger").getDeclaration());
            }

            @Override
            public void visit(NestedChainModel component) {
              declarer.withComponent(core.newOperation("logger").getDeclaration());
            }

            @Override
            public void visit(NestedRouteModel component) {
              RouteElementDeclarer routeDeclarer = core.newRoute(component.getName());
              populateParameterized(component, routeDeclarer);
              routeDeclarer.withComponent(core.newOperation("logger").getDeclaration());

              declarer.withComponent(routeDeclarer.getDeclaration());
            }
          });
        });
      }
    }.walk(extensionModel);

    return artifactDeclarer.getDeclaration();
  }

  private void populateParameterized(ParameterizedModel model, ParameterizedElementDeclarer<?, ?> parameterizedDeclarer) {
    model.getParameterGroupModels()
        .forEach(group -> parameterizedDeclarer.withParameterGroup(groupDeclarer -> {
          AtomicBoolean exclusiveParameterUsed = new AtomicBoolean(false);
          groupDeclarer.withName(group.getName());
          group.getParameterModels()
              .stream()
              .filter(p -> !(p.getType() instanceof UnionType))
              .filter(p -> !p.getModelProperty(QNameModelProperty.class).isPresent())
              .filter(p -> !isExclusiveParameter(group, p) || !exclusiveParameterUsed.get())
              .forEach(param -> {
                if (!exclusiveParameterUsed.get() && isExclusiveParameter(group, param)) {
                  exclusiveParameterUsed.set(true);
                }

                addParameter(param.getType(),
                             isContent(param) || param.getExpressionSupport().equals(REQUIRED),
                             isText(param),
                             allowsReferences(param),
                             ofNullable(param.getDefaultValue()),
                             param.getAllowedStereotypes(),
                             value -> groupDeclarer.withParameter(param.getName(), value));
              });
        }));
  }

  private boolean allowsReferences(ParameterModel param) {
    return param.getDslConfiguration().allowsReferences();
  }

  private boolean isExclusiveParameter(ParameterGroupModel group, ParameterModel param) {
    return group.getExclusiveParametersModels().stream().anyMatch(q -> q.getExclusiveParameterNames().contains(param.getName()));
  }

  private void addParameter(MetadataType type,
                            boolean isContent, boolean isText, boolean allowsReferences,
                            Optional<Object> defaultValue,
                            List<StereotypeModel> allowedStereotypes,
                            Consumer<ParameterValue> valueConsumer) {
    type.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (isContent) {
          valueConsumer.accept(ParameterSimpleValue.of(String.valueOf(defaultValue.orElse("#['ExpressionAttribute']"))));
        } else if (isText) {
          valueConsumer.accept(ParameterSimpleValue.cdata(String.valueOf(defaultValue.orElse("Attribute\nText"))));
        } else {
          String fallback = allowedStereotypes.isEmpty() ? "${placeholder.property}"
              : allowedStereotypes.stream().map(Object::toString).collect(Collectors.joining("|"));
          valueConsumer.accept(ParameterSimpleValue.of(fallback));
        }
      }

      @Override
      public void visitNumber(NumberType numberType) {
        valueConsumer.accept(ParameterSimpleValue.of("${placeholder.property}"));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (isContent) {
          defaultVisit(arrayType);
          return;
        }

        ParameterListValue.Builder listValue = newListValue();
        addParameter(arrayType.getType(), false, false, false, empty(), emptyList(), listValue::withValue);
        addParameter(arrayType.getType(), false, false, false, empty(), emptyList(), listValue::withValue);
        valueConsumer.accept(listValue.build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isContent || allowsReferences || !(supportsInlineDeclaration(objectType) || isWrapped(objectType))) {
          defaultVisit(objectType);
          return;
        }

        ParameterObjectValue.Builder objectValue = newObjectValue();
        getId(objectType).ifPresent(objectValue::ofType);

        objectType.getFields()
            .forEach(field -> addParameter(field.getValue(), false, false, false,
                                           ofNullable(getDefaultValue(field.getValue()).orElse(null)),
                                           emptyList(),
                                           fieldValue -> objectValue.withParameter(getAlias(field), fieldValue)));

        valueConsumer.accept(objectValue.build());
      }
    });
  }

  private Boolean supportsInlineDeclaration(ObjectType objectType) {
    return dslResolver.resolve(objectType).map(DslElementSyntax::supportsChildDeclaration).orElse(false);
  }

  private Boolean isWrapped(ObjectType objectType) {
    return dslResolver.resolve(objectType).map(DslElementSyntax::isWrapped).orElse(false);
  }
}
