/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static java.lang.Boolean.getBoolean;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;

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
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
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
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class BulkArtifactDeclarationTestCase extends AbstractElementModelTestCase {

  private static final String EXPECTED_XML = "core-bulk-extension-model.xml";

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
      getBoolean(SYSTEM_PROPERTY_PREFIX + "appXml.updateExpectedFilesOnError");

  private ArtifactDeclarationXmlSerializer serializer;
  private DslSyntaxResolver dslResolver;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Override
  @Before
  public void setup() throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(extensions)
        .add(MuleExtensionModelProvider.getExtensionModel()).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
    serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
  }

  /**
   * Utility to batch fix input files when severe model changes are introduced. Use carefully, not a mechanism to get away with
   * anything. First check why the generated json is different and make sure you're not introducing any bugs. This should NEVER be
   * committed as true
   *
   * @return whether or not the "expected" test files should be updated when comparison fails
   */
  private boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Test
  public void bulkDeclaration() throws Exception {

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

    String serializationResult = serializer.serialize(artifactDeclarer.getDeclaration());
    String expected = getResourceAsString(EXPECTED_XML, getClass());

    try {
      compareXML(expected, serializationResult);
    } catch (Throwable t) {
      if (shouldUpdateExpectedFilesOnError()) {
        File root = new File(getResourceAsUrl(EXPECTED_XML, getClass()).toURI()).getParentFile()
            .getParentFile().getParentFile();
        File testDir = new File(root, "src/test/resources");
        File target = new File(testDir, EXPECTED_XML);
        stringToFile(target.getAbsolutePath(), serializationResult);

        System.out.println(expected + " fixed");
      }
      throw t;
    }
  }

  private void populateParameterized(ParameterizedModel model, ParameterizedElementDeclarer<?, ?> parameterizedDeclarer) {
    model.getParameterGroupModels()
        .forEach(group -> parameterizedDeclarer.withParameterGroup(groupDeclarer -> {
          groupDeclarer.withName(group.getName());
          group.getParameterModels()
              .stream()
              .filter(p -> !(p.getType() instanceof UnionType))
              .forEach(param -> addParameter(param.getType(),
                                             isContent(param) || param.getExpressionSupport().equals(REQUIRED),
                                             isText(param),
                                             ofNullable(param.getDefaultValue()),
                                             param.getAllowedStereotypes(),
                                             value -> groupDeclarer.withParameter(param.getName(), value)));
        }));
  }

  private void addParameter(MetadataType type, boolean isContent, boolean isText, Optional<Object> defaultValue,
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
          String fallback = allowedStereotypes.isEmpty() ? "Attribute"
              : allowedStereotypes.stream().map(Object::toString).collect(Collectors.joining("|"));
          valueConsumer.accept(ParameterSimpleValue.of(String.valueOf(defaultValue.orElse(fallback))));
        }
      }

      @Override
      public void visitNumber(NumberType numberType) {
        valueConsumer.accept(ParameterSimpleValue.of(String.valueOf(defaultValue.orElse("10000"))));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (isContent) {
          defaultVisit(arrayType);
          return;
        }

        ParameterListValue.Builder listValue = newListValue();
        addParameter(arrayType.getType(), false, false, null, emptyList(), listValue::withValue);
        addParameter(arrayType.getType(), false, false, null, emptyList(), listValue::withValue);
        valueConsumer.accept(listValue.build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isContent || !supportsInlineDeclaration(objectType)) {
          defaultVisit(objectType);
          return;
        }

        ParameterObjectValue.Builder objectValue = newObjectValue();
        getId(objectType).ifPresent(objectValue::ofType);

        objectType.getFields()
            .forEach(field -> addParameter(field.getValue(), false, false,
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
}
