/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
  private static final String NEW_INTERFACE_NOT_PRESENT_IN_DISTRO =
      "org.mule.sdk.api.runtime.connectivity.NonExistingInterfaceInDistribution";
  private static final String METHOD_IN_NEW_INTERFACE = "doSomething";
  private static final String SUCCESS_METHOD_NAME = "success";
  private static final String FAILED_METHOD_NAME = "failed";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/classloading/sdk-api-classloading-mule-config-flow.xml";
  }

  @Test
  public void sdkApiClassLoadingParentResolution() throws Exception {
    List<String> methods =
        (List<String>) flowRunner("getMethods").withPayload(OVERRIDEN_INTERFACE).run().getMessage().getPayload().getValue();
    assertThat(methods, hasSize(2));
    assertThat(methods, containsInAnyOrder(SUCCESS_METHOD_NAME, FAILED_METHOD_NAME));
  }

  @Test
  public void sdkApiClassLoadingChildResolution() throws Exception {
    List<String> methods =
        (List<String>) flowRunner("getMethods").withPayload(NEW_INTERFACE_NOT_PRESENT_IN_DISTRO).run().getMessage().getPayload()
            .getValue();
    assertThat(methods, hasSize(1));
    assertThat(methods, containsInAnyOrder(METHOD_IN_NEW_INTERFACE));
  }
}
