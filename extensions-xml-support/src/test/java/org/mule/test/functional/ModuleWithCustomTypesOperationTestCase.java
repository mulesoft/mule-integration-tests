/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(XML_SDK)
public class ModuleWithCustomTypesOperationTestCase extends AbstractModuleWithHttpTestCase {

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-using-custom-types.xml";
  }

  @Test
  @Issue("MULE-18475")
  public void testSetStreetNameAsPayloadWithEmptyParam() throws Exception {
    assertThat(flowRunner("testSetStreetNameAsPayload").run().getMessage().getPayload().getValue(),
               is(nullValue()));
  }

  @Test
  @Issue("MULE-19976")
  public void testUnionTypeWithNull() throws Exception {
    assertThat(flowRunner("testUnionTypeWithNull").run().getMessage().getPayload().getValue(),
               is("a,b,c"));
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
