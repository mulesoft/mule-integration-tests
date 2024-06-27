/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.beans;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * Just a Spring bean that depends on {@link ExtensionsClient}.
 * <p>
 * The dependency could have actually been any singleton bean in the application registry that depended on
 * {@link MuleConfiguration}.
 * <p>
 * The issue under testing manifested because the bean definition for {@link MuleConfiguration} was being overwritten at some
 * point, invalidating all dependant beans, except those in the spring-module's registry.
 */
public class BeanDependingOnExtensionsClient {

  @Inject
  private ExtensionsClient extensionsClient;

  /**
   * @return The output of vegan:tryEat with an Apple.
   */
  public String getResultFromEatingApple() {
    try {
      // W-15832941: There was an NPE at this point because extensionsClient was not initialized and some of its caches were null
      return (String) extensionsClient
          .execute("vegan",
                   "tryEat",
                   parameterizer -> parameterizer.withParameter("food", new Apple()))
          .get().getOutput();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}
