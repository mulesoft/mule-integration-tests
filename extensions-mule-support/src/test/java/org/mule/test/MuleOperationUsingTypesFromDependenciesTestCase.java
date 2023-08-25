/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider.provideExpressionLanguageMetadataService;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.subtypes.extension.CarDoor;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationUsingTypesFromDependenciesTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-operations-using-types-from-dependencies.xml";
  }

  @Test
  @Description("An operation declaring an output payload type belonging to another extension")
  public void returningTypeFromDependency() throws Exception {
    CoreEvent resultEvent = flowRunner("returningDoorFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getDataType().getType(), is(CarDoor.class));
  }

  @Test
  @Description("An operation declaring a parameter type belonging to another extension")
  public void operationReceivesByParameterWithATypeFromADependency() throws Exception {
    CoreEvent resultEvent = flowRunner("getDoorColorFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("white"));
  }

  @Test
  @Description("An operation declaring a parameter type belonging to another extension and specifying namespace")
  public void operationReceivesByParameterWithATypeFromADependencyDelegatingToDW() throws Exception {
    CoreEvent resultEvent = flowRunner("getDoorColorDelegatingToDwFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("white"));
  }

  @Override
  protected ExpressionLanguageMetadataService getExpressionLanguageMetadataService() {
    return provideExpressionLanguageMetadataService();
  }
}
