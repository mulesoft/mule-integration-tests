/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;

@Issue("W-14067135")
public class HttpRequestErrorExceptionPayloadHandlingTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort unusedPort = new DynamicPort("unusedPort");

  @Override
  protected String getConfigFile() {
    return "http-request-errors-exception-payload-config.xml";
  }

  @Test
  public void connectivity() throws Exception {
    CoreEvent result = flowRunner("handled").withVariable("port", unusedPort.getNumber()).run();
    assertThat(result.getMessage(),
               hasPayload(equalTo(join(lineSeparator(),
                                       "<http:request config-ref=\"simpleConfig\" path=\"testPath\" responseTimeout=\"1000\">",
                                       "<http:headers><![CDATA[",
                                       "#[{'Content-Type': 'application/xml'}]",
                                       "]]></http:headers>",
                                       "</http:request>"))));
  }

}
