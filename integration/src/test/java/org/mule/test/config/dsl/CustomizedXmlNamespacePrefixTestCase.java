/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslParsingStory.DSL_PARSING_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.component.Component;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_PARSING_STORY)
public class CustomizedXmlNamespacePrefixTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/dsl/customized-namespace-prefix-config.xml";
  }

  @Test
  public void validateThatACustomXmlNamespacePrefixCanBeUsed() {
    Optional<Component> httpRequesterOptional = muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("flow").addProcessorsPart().addIndexPart(0).build());
    assertThat(httpRequesterOptional.isPresent(), is(true));
    assertThat(httpRequesterOptional.get().getLocation().getComponentIdentifier().getIdentifier(),
               is(buildFromStringRepresentation("http:request")));
  }
}
