/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.ast.serialization;

import static org.mule.test.allure.AllureConstants.ArtifactAst.ArtifactAstSerialization.AST_SERIALIZATION;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ArtifactAstSerialization.AST_SERIALIZATION_END_TO_END;

import static java.lang.String.format;
import static java.util.Collections.singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.internal.serialization.json.JsonArtifactAstSerializerFormat;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;

import java.io.IOException;
import java.io.InputStream;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@Feature(AST_SERIALIZATION)
@Story(AST_SERIALIZATION_END_TO_END)
public class OldArtifactAstWithNewSerializerAndRuntimeExtensionModelsTestCase {

  @Parameters(name = "version: {0}")
  public static Iterable<String> data() {
    return new JsonArtifactAstSerializerFormat().getAvailableVersions();
  }

  private static final ExtensionModel RUNTIME_EXTENSION_MODEL = MuleExtensionModelProvider.getExtensionModel();
  private static final String APP_RESOURCE_PATH = "apps/app-with-choice.xml";

  @Parameter
  public String version;

  private final ArtifactAstSerializerProvider serializerProvider = new ArtifactAstSerializerProvider();
  private ArtifactAst artifactAst;
  private InputStream serializedAst;

  @Before
  public void setup() {
    artifactAst = createArtifactFromXmlFile(APP_RESOURCE_PATH);
    ArtifactAstSerializer serializer = serializerProvider.getSerializer("JSON", version);
    serializedAst = serializer.serialize(artifactAst);
  }

  @Test
  @Issue("W-14722981")
  public void parametersAllowingExpressionsWithoutMarkersAreCorrectlyLoadedFromXML() {
    // Control test: ArtifactAst already has the wrong value for the parameter that allowed expressions without markers
    // This happened because the version of the ArtifactAst classes do not match with the version of the Runtime Extension Model
    Either<String, Boolean> choiceParameterValue = getChoiceParameterValue(artifactAst);
    assertThat(choiceParameterValue.isLeft(), is(true));
    assertThat(choiceParameterValue.getLeft(), is("0 != 1"));
  }

  @Test
  @Issue("W-14722981")
  public void parametersAllowingExpressionsWithoutMarkersAreSerializedCorrectly()
      throws IOException {
    consumeHeaderLine(serializedAst);

    JSONObject choiceParameterValueJson = getChoiceParameterValue(serializedAst);
    assertThat(choiceParameterValueJson.has("expression"), is(true));
    assertThat(choiceParameterValueJson.getString("expression"), is("0 != 1"));
  }

  @Test
  @Issue("W-14722981")
  public void parametersAllowingExpressionsWithoutMarkersAreDeserializedCorrectly()
      throws IOException {
    ArtifactAst deserializedAst = serializerProvider.getDeserializer().deserialize(serializedAst, this::resolveExtensionModel);
    Either<String, Boolean> choiceParameterValue = getChoiceParameterValue(deserializedAst);

    // No matter the serialized version, it must always be deserialized to the correct value
    assertThat(choiceParameterValue.isLeft(), is(true));
    assertThat(choiceParameterValue.getLeft(), is("0 != 1"));
  }

  private void consumeHeaderLine(InputStream serializedAst) throws IOException {
    int c;
    // noinspection StatementWithEmptyBody
    while ((c = serializedAst.read()) != -1 && c != '\n') {
      // does nothing
    }
  }

  private Either<String, Boolean> getChoiceParameterValue(ArtifactAst artifactAst) {
    ComponentAst flow = artifactAst.topLevelComponents().get(0);
    ComponentAst choice = flow.directChildren().get(0);
    ComponentAst when = choice.directChildren().get(0);
    ComponentParameterAst expressionParameter = when.getParameter("General", "expression");
    return expressionParameter.getValue();
  }

  private JSONObject getChoiceParameterValue(InputStream serializedAst) {
    JSONTokener jsonTokener = new JSONTokener(serializedAst);
    JSONObject flow = new JSONObject(jsonTokener).getJSONArray("topLevelComponentAsts").getJSONObject(0);
    JSONObject choice = flow.getJSONArray("directChildren").getJSONObject(0);
    JSONObject when = choice.getJSONArray("directChildren").getJSONObject(0);
    JSONObject expressionParameter = when.getJSONArray("parameters").getJSONObject(0);
    return expressionParameter.getJSONObject("value");
  }

  private ArtifactAst createArtifactFromXmlFile(String xmlResourcePath) {
    AstXmlParser xmlParser = new AstXmlParser.Builder()
        .withExtensionModels(singleton(RUNTIME_EXTENSION_MODEL))
        .withPropertyResolver(p -> p)
        .build();

    return xmlParser.parse(this.getClass().getClassLoader().getResource(xmlResourcePath));
  }

  public ExtensionModel resolveExtensionModel(String name) throws IllegalArgumentException {
    if (RUNTIME_EXTENSION_MODEL.getName().equals(name)) {
      return RUNTIME_EXTENSION_MODEL;
    }
    throw new IllegalArgumentException(format("Extension model '%s' could not be resolved", name));
  }
}
