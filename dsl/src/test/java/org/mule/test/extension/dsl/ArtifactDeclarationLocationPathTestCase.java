/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.functional.junit4.ArtifactAstXmlParserConfigurationBuilder.SERIALIZE_DESERIALIZE_AST_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.ast.api.DependencyResolutionMode.MINIMAL;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConstructElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.component.location.Location;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.ast.api.DependencyResolutionMode;
import org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ArtifactDeclarationLocationPathTestCase extends AbstractElementModelTestCase {

  @Rule
  public SystemProperty minimalDependencies =
      new SystemProperty(SYSTEM_PROPERTY_PREFIX + DependencyResolutionMode.class.getName(), MINIMAL.name());

  public static final String ORIGINAL_CONFIG = "multi-flow-dsl-app.xml";
  public static final String EXPECTED_UPDATED_CONFIG = "location-path-update-multi-flow-dsl-app.xml";
  public ArtifactDeclaration multiFlowDeclaration;
  private ArtifactDeclarationXmlSerializer serializer;

  @Override
  protected String getConfigFile() {
    return ORIGINAL_CONFIG;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    // this case tests a configuration of the ast generator that is not compatible with the serialization.
    assumeThat(getBoolean(SERIALIZE_DESERIALIZE_AST_PROPERTY), is(false));
    super.doSetUpBeforeMuleContextCreation();
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
    multiFlowDeclaration = serializer.deserialize(getConfigFile(),
                                                  currentThread().getContextClassLoader()
                                                      .getResourceAsStream(getConfigFile()));
  }

  @Test
  public void updatePropertyAndSerialize() throws Exception {
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");

    final Location destinationLocation = Location.builder()
        .globalName("send-payload")
        .addProcessorsPart()
        .addIndexPart(0)
        .addParameterPart()
        .addPart("destination")
        .build();

    Optional<ParameterElementDeclaration> destination = multiFlowDeclaration.findElement(destinationLocation);
    assertThat(destination.isPresent(), is(true));
    destination.get().setValue(ParameterSimpleValue.of("updatedDestination"));

    final Location flowLocation = Location.builder().globalName("send-payload").build();
    Optional<ConstructElementDeclaration> flow = multiFlowDeclaration.findElement(flowLocation);
    assertThat(destination.isPresent(), is(true));
    flow.get().addComponent(0, jms.newSource("listener")
        .withConfig("config")
        .withParameterGroup(newParameterGroup()
            .withParameter("destination", "myListenerDestination")
            .getDeclaration())
        .getDeclaration());

    String serialized = serializer.serialize(multiFlowDeclaration);
    compareXML(getResourceAsString(EXPECTED_UPDATED_CONFIG, getClass()), serialized);
  }
}
