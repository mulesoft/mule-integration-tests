/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

@Issue("MULE-17051")
public class MuleConfigurationIntegrationTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("httpPort");

  @Rule
  public SystemProperty EXPECTED_PAYLOAD = new SystemProperty("someValue", "No tengo pruebas, pero tampoco dudas");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler-config-default.xml";
  }

  @Test
  public void modifyingDefaultErrorHandlerCanBeDeployed() throws Exception {
    CoreEvent event = flowRunner("someFlow").keepStreamsOpen().run();
    CursorStreamProvider provider = (CursorStreamProvider) event.getMessage().getPayload().getValue();
    assertThat(IOUtils.toString(provider), is(EXPECTED_PAYLOAD.getValue()));
  }

}
