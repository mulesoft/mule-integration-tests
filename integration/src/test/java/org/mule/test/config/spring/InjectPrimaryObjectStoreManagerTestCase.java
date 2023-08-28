/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunnerDelegateTo(MockitoJUnitRunner.class)
public class InjectPrimaryObjectStoreManagerTestCase extends AbstractIntegrationTestCase {

  @Mock(lenient = true)
  private ObjectStore overridenBaseStore;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY, overridenBaseStore);
        muleContext.getCustomizationService().overrideDefaultServiceImpl(BASE_PERSISTENT_OBJECT_STORE_KEY, overridenBaseStore);
      }

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {}
    });
  }

  @Test
  public void twoManagersRegistered() {
    assertThat(registry.lookupAllByType(ObjectStoreManager.class), hasSize(2));
  }

  @Test
  public void injectPrimaryObjectStoreManager() throws Exception {
    InjectionTarget target = new InjectionTarget();
    muleContext.getInjector().inject(target);

    assertThat(target.getObjectStoreManager(), is(notNullValue()));
  }


  public static class InjectionTarget {

    @Inject
    private ObjectStoreManager objectStoreManager;

    public ObjectStoreManager getObjectStoreManager() {
      return objectStoreManager;
    }
  }
}
