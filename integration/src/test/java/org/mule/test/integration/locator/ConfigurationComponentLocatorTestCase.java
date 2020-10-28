/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.source.SchedulerMessageSource.SCHEDULER_MESSAGE_SOURCE_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class ConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-config.xml";
  }

  @Description("Search for a global component that do not exists return an empty optional")
  @Test
  public void globalObjectNotFound() {
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("nonExistent").build()).isPresent(),
               is(false));
  }

  @Description("Search for a global component found returns an Optional with the proper value")
  @Test
  public void globalObjectFound() {
    Optional<Component> myFlow =
        muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build());
    assertThat(myFlow.isPresent(), is(true));
    assertThat(myFlow.get(), instanceOf(Flow.class));
  }

  @Description("Search for a component with an non existent global component part returns an empty optional")
  @Test
  public void badContainerType() {
    Location location = builder().globalName("pepe").addProcessorsPart().addIndexPart(0).build();
    assertThat(muleContext.getConfigurationComponentLocator().find(location).isPresent(), is(false));
  }

  @Description("Search for a message source component by the flow component name")
  @Test
  public void sourceByPath() {
    Location sourceLocation = builder().globalName("myFlow").addSourcePart().build();
    Optional<Component> source = muleContext.getConfigurationComponentLocator().find(sourceLocation);
    assertThat(source.isPresent(), is(true));
    assertThat(source.get(), instanceOf(MessageSource.class));
  }

  @Description("Search message processors components within a flow")
  @Test
  public void messageProcessorByPath() {
    Location.Builder myFlowProcessorsLocationBuilder = builder().globalName("myFlow").addProcessorsPart();
    Optional<Component> processor =
        muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get().getClass().getName(), equalTo("org.mule.runtime.core.internal.processor.LoggerMessageProcessor"));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get().getClass().getName(),
               equalTo("org.mule.runtime.core.internal.processor.simple.SetPayloadMessageProcessor"));
    processor = muleContext.getConfigurationComponentLocator().find(myFlowProcessorsLocationBuilder.addIndexPart(2).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get().getClass().getName(),
               equalTo("org.mule.runtime.core.internal.processor.AsyncDelegateMessageProcessor"));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(0).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get().getClass().getName(),
               equalTo("org.mule.runtime.core.internal.processor.simple.SetPayloadMessageProcessor"));
    processor = muleContext.getConfigurationComponentLocator()
        .find(myFlowProcessorsLocationBuilder.addIndexPart(2).addProcessorsPart().addIndexPart(1).build());
    assertThat(processor.isPresent(), is(true));
    assertThat(processor.get().getClass().getName(), equalTo("org.mule.runtime.core.internal.processor.LoggerMessageProcessor"));
  }

  @Description("Search all scheduler message sources within the configuration")
  @Test
  public void findAllSchedulers() {
    List<Component> components = muleContext.getConfigurationComponentLocator().find(SCHEDULER_MESSAGE_SOURCE_IDENTIFIER);
    assertThat(components, hasSize(3));
    assertThat(components.stream().map(component -> component.getLocation().getRootContainerName()).collect(toList()),
               hasItems("myFlow", "anotherFlow"));
  }

  @Description("Search by ComponentIdentifier returns empty collection when no elements could be found")
  @Test
  public void notFoundComponentByComponentIdentifierReturnsEmptyCollection() {
    List<Component> components =
        muleContext.getConfigurationComponentLocator().find(buildFromStringRepresentation("mule:nonExistent"));
    assertThat(components, notNullValue());
    assertThat(components, hasSize(0));
  }

  @Description("Search for all the components in the configuration")
  @Test
  public void findAllComponents() {
    List<ComponentLocation> componentLocs = muleContext.getConfigurationComponentLocator().findAllLocations();
    List<String> allComponentPaths = componentLocs.stream().map(ComponentLocation::getLocation).collect(toList());
    assertThat(allComponentPaths, containsInAnyOrder(
                                                     "myFlow",
                                                     "invokeBeanFlow",
                                                     "invokeBeanFlow/processors/0",
                                                     "childBean",
                                                     "myFlow/source",
                                                     "myFlow/processors/0",
                                                     "myFlow/processors/1",
                                                     "myFlow/processors/2",
                                                     "myFlow/processors/2/processors/0",
                                                     "myFlow/processors/2/processors/1",
                                                     "anotherFlow",
                                                     "anotherFlow/source",
                                                     "anotherFlow/processors/0",
                                                     "flowWithSubflow",
                                                     "flowWithSubflow/processors/0",
                                                     "mySubFlow",
                                                     "mySubFlow/processors/0",
                                                     "flowFailing",
                                                     "flowFailing/processors/0",
                                                     "_muleConfiguration",
                                                     "globalErrorHandler",
                                                     "globalErrorHandler/0",
                                                     "globalErrorHandler/0/processors/0",
                                                     "globalErrorHandler/0/processors/1",
                                                     "fileListWithMatcherReference",
                                                     "fileListWithMatcherReference/source",
                                                     "fileListWithMatcherReference/processors/0",
                                                     "Matcher",
                                                     "listenerConfigRedeliveryPolicy",
                                                     "listenerConfigRedeliveryPolicy/0",
                                                     "redeliveryPolicyFlow",
                                                     "redeliveryPolicyFlow/source",
                                                     "redeliveryPolicyFlow/source/0",
                                                     "redeliveryPolicyFlow/processors/0",
                                                     "untilSuccessfulFlow",
                                                     "untilSuccessfulFlow/processors/0",
                                                     "untilSuccessfulFlow/processors/0/processors/0",
                                                     "untilSuccessfulFlowCopy",
                                                     "untilSuccessfulFlowCopy/processors/0",
                                                     "untilSuccessfulFlowCopy/processors/0/processors/0",
                                                     "multipleInitialize",
                                                     "multipleInitialize/processors/0",
                                                     "multipleInitialize/processors/1",
                                                     "async-flow/processors/0/processors/0",
                                                     "async-flow/processors/0",
                                                     "async-flow"));
  }
}
