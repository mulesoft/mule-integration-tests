/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.classloading;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class SdkApiClassloadingTestCase extends AbstractIntegrationTestCase {

  private static final String OVERRIDEN_INTERFACE = "org.mule.sdk.api.runtime.connectivity.ReconnectionCallback";
  private static final String INTERFACE_NEW_METHOD_NAME = "doSomething";
  private static final String SUCCESS_METHOD_NAME = "success";
  private static final String FAILED_METHOD_NAME = "failed";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/classloading/sdk-api-classloading-mule-config-flow.xml";
  }

  @Test
  public void testSdkApiClassLoading() throws Exception {
    List<String> methods =
        (List<String>) flowRunner("getMethods").withPayload(OVERRIDEN_INTERFACE).run().getMessage().getPayload().getValue();
    assertThat(methods, hasSize(3));
    assertThat(methods, containsInAnyOrder(INTERFACE_NEW_METHOD_NAME, SUCCESS_METHOD_NAME, FAILED_METHOD_NAME));

  }
}
