/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.ObjectTag.OBJECT_TAG_FEATURE;
import static org.mule.test.allure.AllureConstants.ObjectTag.ObjectPropertiesStory.OBJECT_PROPERTIES;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

@Feature(OBJECT_TAG_FEATURE)
@Story(OBJECT_PROPERTIES)
public class ObjectPropertiesExtensionModelTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "object-properties.xml";
  }

  @Test
  @Issue("MULE-19545")
  public void propertiesAreCorrectlySetted() throws Exception {
    CoreEvent event = flowRunner("test").run();
    assertThat(event.getMessage().getPayload().getValue(),
               is("El campeón de la Copa América es Argentina, gracias a Lionel Messi"));
  }

  public static class TestDto implements Processor {

    private String champion;
    private String keyPlayer;

    public void setChampion(String champ) {
      this.champion = champ;
    }

    public void setKeyPlayer(String keyPlayer) {
      this.keyPlayer = keyPlayer;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return CoreEvent.builder(event)
          .message(Message.of(String.format("El campeón de la Copa América es %s, gracias a %s", champion, keyPlayer))).build();
    }
  }
}
