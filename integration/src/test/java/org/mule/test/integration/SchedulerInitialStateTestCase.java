/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.deployment.management.ComponentInitialStateManager.SERVICE_ID;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.SOURCE_MANAGEMENT;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SCHEDULER_SERVICE)
@Story(SOURCE_MANAGEMENT)
@RunnerDelegateTo(Parameterized.class)
public class SchedulerInitialStateTestCase extends AbstractIntegrationTestCase {

  private final List<Component> recordedOnStartMessageSources = new ArrayList<>();

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Inject
  private ConfigurationComponentLocator configurationComponentLocator;

  @Inject
  private TestQueueManager queueManager;

  @Parameterized.Parameter
  public boolean lazyInitEnabled;

  @Parameterized.Parameters(name = "lazyInit: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[] {true}, new Object[] {false});
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-initial-state-management-config.xml";
  }

  public boolean isLazyInitEnabled() {
    return lazyInitEnabled;
  }

  @Override
  public boolean disableXmlValidations() {
    return lazyInitEnabled;
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(SERVICE_ID, createCustomStateManager());
      }

      private ComponentInitialStateManager createCustomStateManager() {
        return new ComponentInitialStateManager() {

          @Override
          public boolean mustStartMessageSource(Component component) {
            recordedOnStartMessageSources.add(component);
            return component.getLocation().getRootContainerName().equals("runningSchedulerOnStartup");
          }
        };
      }

      @Override
      public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
        // Nothing to do
      }
    });
  }

  @Description("ComponentInitialStateManager is called during startup for all message sources")
  @Test
  public void startMessageSourceRequestedOnStartup() {
    lazyComponentInitializer.initializeComponents(componentLocation -> true);
    assertThat(recordedOnStartMessageSources, hasSize(2));
    assertThat(recordedOnStartMessageSources.stream()
        .map(component -> component.getLocation().getLocation()).collect(toList()),
               hasItems("runningSchedulerOnStartup/source", "notRunningSchedulerOnStartup/source"));
  }

  @Description("ComponentInitialStateManager does not allow to start scheduler message sources")
  @Test
  public void verifyMessageSourcesAreNotStarted() throws InterruptedException {
    lazyComponentInitializer.initializeComponents(componentLocation -> true);
    SchedulerMessageSource schedulerMessageSource = (SchedulerMessageSource) configurationComponentLocator
        .find(Location.builder().globalName("runningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(true));
    schedulerMessageSource = (SchedulerMessageSource) configurationComponentLocator
        .find(Location.builder().globalName("notRunningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(false));

    assertThat(queueManager.read("runningSchedulerOnStartupQueue", RECEIVE_TIMEOUT, MILLISECONDS),
               is(not(nullValue())));

    assertThat(queueManager.read("notRunningSchedulerOnStartupQueue", 100, MILLISECONDS),
               is(nullValue()));
  }
}
