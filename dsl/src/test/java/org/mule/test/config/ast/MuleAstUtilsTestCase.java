/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.ast;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.util.MuleAstUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(ARTIFACT_AST)
public class MuleAstUtilsTestCase extends AbstractMuleContextTestCase {

  @Test
  public void emptyArtifactHasCoreExtModelDependency() {
    final Set<ExtensionModel> dependencies = MuleAstUtils.emptyArtifact().dependencies();

    assertThat(dependencies, hasSize(1));
    assertThat(dependencies.iterator().next().getName(), is(MULE_NAME));
  }
}
