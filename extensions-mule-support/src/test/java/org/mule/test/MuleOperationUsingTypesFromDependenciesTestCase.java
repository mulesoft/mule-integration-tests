/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.subtypes.extension.CarDoor;
import org.mule.weave.v2.el.metadata.WeaveExpressionLanguageMetadataServiceImpl;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

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
    CoreEvent resultEvent = flowRunner("getDoorColorDelegatingFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("white"));
  }

  @Override
  protected ExpressionLanguageMetadataService getExpressionLanguageMetadataService() {
    return new WeaveExpressionLanguageMetadataServiceImpl();
  }
}
