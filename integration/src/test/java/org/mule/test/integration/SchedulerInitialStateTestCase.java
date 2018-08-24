/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.deployment.management.ComponentInitialStateManager.SERVICE_ID;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SCHEDULER_SERVICE;
import static org.mule.test.allure.AllureConstants.SchedulerServiceFeature.SchedulerServiceStory.SOURCE_MANAGEMENT;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(SCHEDULER_SERVICE)
@Story(SOURCE_MANAGEMENT)
@RunnerDelegateTo(Parameterized.class)
public class SchedulerInitialStateTestCase extends AbstractIntegrationTestCase {

  private List<Component> recordedOnStartMessageSources = new ArrayList<>();

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Parameterized.Parameter
  public boolean lazyInitEnabled;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} (validating XML [{2}]) ")
  public static Collection<Object[]> data() {
    return asList(new Object[] {true}, new Object[] {false});
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-initial-state-management-config.xml";
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    final ConfigurationBuilder configurationBuilder = createConfigurationBuilder(getConfigFile(), lazyInitEnabled);
    configureSpringXmlConfigurationBuilder(configurationBuilder);
    return configurationBuilder;
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
  public void verifyMessageSourcesAreNotStarted() {
    lazyComponentInitializer.initializeComponents(componentLocation -> true);
    SchedulerMessageSource schedulerMessageSource = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("runningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(true));
    schedulerMessageSource = (SchedulerMessageSource) muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("notRunningSchedulerOnStartup").addSourcePart().build()).get();
    assertThat(schedulerMessageSource.isStarted(), is(false));

    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);

    new PollingProber(10000, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        Event response = queueHandler.read("runningSchedulerOnStartupQueue", 100);
        return response != null;
      }

      @Override
      public String describeFailure() {
        return "Message expected by in flow runningSchedulerOnStartup";
      }
    });

    Event response = queueHandler.read("notRunningSchedulerOnStartupQueue", 100);
    assertThat(response, is(nullValue()));
  }
}
