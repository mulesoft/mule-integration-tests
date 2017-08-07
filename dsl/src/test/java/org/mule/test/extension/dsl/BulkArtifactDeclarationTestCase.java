/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.app.declaration.ParameterValue;
import org.mule.runtime.api.app.declaration.fluent.ArtifactDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ConfigurationElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ConnectionElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ConstructElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.OperationElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ParameterListValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterObjectValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterSimpleValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterizedElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.RouteElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.SourceElementDeclarer;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisistor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.config.spring.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.spring.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

public class BulkArtifactDeclarationTestCase extends AbstractElementModelTestCase {


  private ArtifactDeclarationXmlSerializer serializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Before
  public void setup() throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(extensions)
        .add(MuleExtensionModelProvider.getExtensionModel()).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
    serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
  }

  @Test
  public void bulkDeclaration() throws Exception {

    ExtensionModel extensionModel = MuleExtensionModelProvider.getExtensionModel();
    ElementDeclarer core = ElementDeclarer.forExtension(extensionModel.getName());
    ArtifactDeclarer artifactDeclarer = newArtifact();

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

        model.getNestedComponents().forEach(nestedPlaceholder -> {
          nestedPlaceholder.accept(new NestableElementModelVisistor() {

            @Override
            public void visit(NestedComponentModel component) {
              declarer.withComponent(core.newOperation("logger").getDeclaration());
            }

            @Override
            public void visit(NestedChainModel component) {
              declarer.withComponent(core.newOperation("logger").getDeclaration());
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

        if (model.allowsTopLevelDeclaration()) {
          artifactDeclarer.withGlobalElement(declarer.withRefName("global" + model.getName()).getDeclaration());
        } else {
          artifactDeclarer.withGlobalElement(core.newConstruct("flow")
              .withRefName("flowFor" + model.getName())
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
        artifactDeclarer.withGlobalElement(core.newConstruct("flow")
            .withRefName("flowFor" + model.getName())
            .withComponent(declarer.getDeclaration())
            .getDeclaration());
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        SourceElementDeclarer declarer = core.newSource(model.getName());
        populateParameterized(model, declarer);
        artifactDeclarer.withGlobalElement(core.newConstruct("flow")
            .withRefName("flowFor" + model.getName())
            .withComponent(declarer.getDeclaration())
            .getDeclaration());
      }
    }.walk(extensionModel);

    String serializationResult = serializer.serialize(artifactDeclarer.getDeclaration());
    String expected = getResourceAsString("core-bulk-extension-model.xml", getClass());

    compareXML(expected, serializationResult);

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
                                             Optional.ofNullable(param.getDefaultValue()),
                                             value -> groupDeclarer.withParameter(param.getName(), value)));
        }));
  }

  private void addParameter(MetadataType type, boolean isContent, boolean isText, Optional<Object> defaultValue,
                            Consumer<ParameterValue> valueConsumer) {

    type.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (isContent) {
          valueConsumer.accept(ParameterSimpleValue.of(String.valueOf(defaultValue.orElse("#['ExpressionAttribute']"))));
        } else if (isText) {
          valueConsumer.accept(ParameterSimpleValue.cdata(String.valueOf(defaultValue.orElse("Attribute\nText"))));
        } else {
          valueConsumer.accept(ParameterSimpleValue.of(String.valueOf(defaultValue.orElse("Attribute"))));
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
        addParameter(arrayType.getType(), false, false, null, listValue::withValue);
        addParameter(arrayType.getType(), false, false, null, listValue::withValue);
        valueConsumer.accept(listValue.build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isContent) {
          defaultVisit(objectType);
          return;
        }

        ParameterObjectValue.Builder objectValue = newObjectValue()
            .ofType(ExtensionMetadataTypeUtils.getId(objectType));

        objectType.getFields()
            .forEach(field -> addParameter(field.getValue(), false, false,
                                           Optional.ofNullable(getDefaultValue(field).orElse(null)),
                                           fieldValue -> objectValue.withParameter(getAlias(field), fieldValue)));

        valueConsumer.accept(objectValue.build());
      }
    });
  }
}
