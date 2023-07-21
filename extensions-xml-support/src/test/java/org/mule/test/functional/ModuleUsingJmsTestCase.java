/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.runtime.api.message.Message;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class ModuleUsingJmsTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-using-jms.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-jms.xml";
  }

  @Test
  public void publishAndConsumeOnce() throws Exception {
    final String content = "a message";
    // produce the message in the queue
    flowRunner("producer-flow")
        .withVariable("content", content)
        .run();
    // consume the message from the queue
    final Message consumedMessage = flowRunner("consumer-flow").run().getMessage();
    assertThat(consumedMessage, hasPayload(equalTo(content)));
  }

}
