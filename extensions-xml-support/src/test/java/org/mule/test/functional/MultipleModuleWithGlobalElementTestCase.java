/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.junit.Test;

import io.qameta.allure.Feature;

/**
 * Guarantees that the macro expansion works for direct dependency on the module but also with the nested one
 */
@Feature(XML_SDK)
public class MultipleModuleWithGlobalElementTestCase extends AbstractModuleWithHttpTestCase {

  @Override
  protected String[] getModulePaths() {
    return new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML, MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML};
  }

  @Override
  protected String getConfigFile() {
    return "flows/nested/flows-using-module-global-elements-another-proxy-and-module-global-elements.xml";
  }

  @Test
  public void testHttpDoLoginThroughNestedModules() throws Exception {
    assertFlowForUsername("testHttpDoLoginThroughNestedModules", "nestedUser");
  }

  @Test
  public void testHttpDoLoginThroughDirectModule() throws Exception {
    assertFlowForUsername("testHttpDoLoginThroughDirectModule", "directUser");
  }
}
