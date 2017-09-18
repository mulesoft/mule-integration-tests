/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.INTERCEPTING;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.SUBFLOW_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocationStory.COMPONENT_LOCATION;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.notification.MessageProcessorNotification;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(COMPONENT_LOCATION)
public class ComponentLocationTestCase extends AbstractIntegrationTestCase {

  private static final Optional<TypedComponentIdentifier> FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().identifier(FLOW_IDENTIFIER).type(FLOW).build());

  private static final Optional<TypedComponentIdentifier> SUB_FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().identifier(SUBFLOW_IDENTIFIER).type(SCOPE).build());

  private static final Optional<String> CONFIG_FILE_NAME =
      of("org/mule/test/integration/notifications/component-path-test-flow.xml");

  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      new DefaultComponentLocation(of("flowWithSingleMp"),
                                   asList(new DefaultLocationPart("flowWithSingleMp",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(27))));
  private static final DefaultComponentLocation FLOW_WITH_MULTIPLE_MP_LOCATION =
      new DefaultComponentLocation(of("flowWithMultipleMps"),
                                   asList(new DefaultLocationPart("flowWithMultipleMps",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(31))));
  private static final DefaultComponentLocation FLOW_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(of("flowWithErrorHandler"),
                                   asList(new DefaultLocationPart("flowWithErrorHandler",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(37))));
  private static final DefaultComponentLocation FLOW_WITH_BLOCK_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(of("flowWithTryWithErrorHandler"),
                                   asList(new DefaultLocationPart("flowWithTryWithErrorHandler",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(58))));

  private static final DefaultComponentLocation FLOW_WITH_SOURCE =
      new DefaultComponentLocation(of("flowWithSource"),
                                   asList(new DefaultLocationPart("flowWithSource",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(70))));

  private static final DefaultComponentLocation FLOW_WITH_SPLITTER =
      new DefaultComponentLocation(of("flowWithSplitter"),
                                   asList(new DefaultLocationPart("flowWithSplitter",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(75))));

  private static final DefaultComponentLocation FLOW_WITH_AGGREGATOR =
      new DefaultComponentLocation(of("flowWithAggregator"),
                                   asList(new DefaultLocationPart("flowWithAggregator",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(81))));

  private static final DefaultComponentLocation FLOW_WITH_SCATTER_GATHER =
      new DefaultComponentLocation(of("flowWithScatterGather"),
                                   asList(new DefaultLocationPart("flowWithScatterGather",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(98))));

  private static final DefaultComponentLocation FLOW_WITH_ASYNC =
      new DefaultComponentLocation(of("flowWithAsync"),
                                   asList(new DefaultLocationPart("flowWithAsync",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(112))));

  private static final DefaultComponentLocation FLOW_WITH_SUBFLOW =
      new DefaultComponentLocation(of("flowWithSubflow"),
                                   asList(new DefaultLocationPart("flowWithSubflow",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(87))));


  private static final DefaultComponentLocation SUBFLOW =
      new DefaultComponentLocation(of("subflow"),
                                   asList(new DefaultLocationPart("subflow",
                                                                  SUB_FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(93))));
  private static final Optional<TypedComponentIdentifier> LOGGER =
      of(builder().identifier(buildFromStringRepresentation("mule:logger"))
          .type(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> SET_PAYLOAD =
      of(builder().identifier(buildFromStringRepresentation("mule:set-payload"))
          .type(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> OBJECT_TO_STRING_TRANSFORMER =
      of(builder().identifier(buildFromStringRepresentation("mule:object-to-byte-array-transformer")).type(PROCESSOR)
          .build());
  private static final Optional<TypedComponentIdentifier> CHOICE =
      of(builder().identifier(buildFromStringRepresentation("mule:choice")).type(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> ERROR_HANDLER =
      of(builder().identifier(buildFromStringRepresentation("mule:error-handler"))
          .type(TypedComponentIdentifier.ComponentType.ERROR_HANDLER).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_CONTINUE =
      of(builder().identifier(buildFromStringRepresentation("mule:on-error-continue")).type(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_FALSE =
      of(builder().identifier(buildFromStringRepresentation("validation:is-false")).type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> TEST_PROCESSOR =
      of(builder().identifier(buildFromStringRepresentation("test:processor")).type(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_PROPAGATE =
      of(builder().identifier(buildFromStringRepresentation("mule:on-error-propagate")).type(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> TRY =
      of(builder().identifier(buildFromStringRepresentation("mule:try")).type(SCOPE).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_TRUE =
      of(builder().identifier(buildFromStringRepresentation("validation:is-true")).type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> SKELETON_SOURCE =
      of(builder().identifier(buildFromStringRepresentation("test:skeleton-source")).type(SOURCE).build());
  private static final Optional<TypedComponentIdentifier> SPLITTER =
      of(builder().identifier(buildFromStringRepresentation("mule:splitter")).type(INTERCEPTING).build());
  private static final Optional<TypedComponentIdentifier> COLLECTION_AGGREGATOR =
      of(builder().identifier(buildFromStringRepresentation("mule:collection-aggregator")).type(INTERCEPTING).build());
  private static final Optional<TypedComponentIdentifier> SCATTER_GATHER =
      of(builder().identifier(buildFromStringRepresentation("mule:scatter-gather")).type(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> FLOW_REF =
      of(builder().identifier(buildFromStringRepresentation("mule:flow-ref")).type(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> ASYNC =
      of(builder().identifier(buildFromStringRepresentation("mule:async")).type(SCOPE).build());
  private static final Optional<TypedComponentIdentifier> ROUTE =
      of(builder().identifier(buildFromStringRepresentation("mule:route")).type(SCOPE).build());

  @Override
  protected String getConfigFile() {
    return CONFIG_FILE_NAME.get();
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(28)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithMultipleMps() throws Exception {
    flowRunner("flowWithMultipleMps").run();
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(32)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, CONFIG_FILE_NAME, of(33)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", OBJECT_TO_STRING_TRANSFORMER,
                            CONFIG_FILE_NAME, of(34)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnContinue() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", false).run();
    DefaultComponentLocation choiceLocation = FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38));
    assertNextProcessorLocationIs(choiceLocation);
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER,
                            CONFIG_FILE_NAME, of(46))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(47))
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(48)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnPropagate() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", true).runExpectingException();
    DefaultComponentLocation choiceLocation = FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38));
    assertNextProcessorLocationIs(choiceLocation);
    DefaultComponentLocation choiceRoute0 = choiceLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(39));
    assertNextProcessorLocationIs(choiceRoute0
        .appendProcessorsPart()
        .appendLocationPart("0", TEST_PROCESSOR, CONFIG_FILE_NAME, of(40)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME, of(50))
        .appendProcessorsPart()
        .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(51)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME,
                            of(50))
        .appendProcessorsPart()
        .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(51))
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(52)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithBlockWithErrorHandler() throws Exception {
    flowRunner("flowWithTryWithErrorHandler").run();
    DefaultComponentLocation blockLocation =
        FLOW_WITH_BLOCK_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), empty())
            .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(59));
    assertNextProcessorLocationIs(blockLocation);
    assertNextProcessorLocationIs(blockLocation
        .appendProcessorsPart()
        .appendLocationPart("0", TEST_PROCESSOR, CONFIG_FILE_NAME, of(60)));
    DefaultComponentLocation blockOnErrorContinueLocation = blockLocation
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(61))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(62));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME, of(63)));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendProcessorsPart()
        .appendLocationPart("1", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(64)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSource() throws Exception {
    DefaultComponentLocation expectedSourceLocation =
        FLOW_WITH_SOURCE.appendLocationPart("source", SKELETON_SOURCE,
                                            CONFIG_FILE_NAME, of(71));
    Flow flowWithSource = (Flow) getFlowConstruct("flowWithSource");
    DefaultComponentLocation sourceLocation =
        (DefaultComponentLocation) ((Component) flowWithSource.getSource()).getAnnotation(LOCATION_KEY);
    assertThat(sourceLocation, is(expectedSourceLocation));
    assertThat(((Component) flowWithSource.getProcessors().get(0)).getAnnotation(LOCATION_KEY), is(FLOW_WITH_SOURCE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(72))));
  }

  @Test
  public void flowWithSplitter() throws Exception {
    flowRunner("flowWithSplitter").withPayload(Arrays.asList("item")).run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SPLITTER.appendLocationPart("processors", empty(), empty(), empty());
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", SPLITTER, CONFIG_FILE_NAME,
                            of(76)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", LOGGER, CONFIG_FILE_NAME, of(77)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", LOGGER, CONFIG_FILE_NAME, of(78)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithAggregator() throws Exception {
    flowRunner("flowWithAggregator").withPayload(Arrays.asList("item")).run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_AGGREGATOR.appendLocationPart("processors", empty(), empty(), empty());
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", SPLITTER, CONFIG_FILE_NAME, of(82)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", LOGGER, CONFIG_FILE_NAME, of(83)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", COLLECTION_AGGREGATOR, CONFIG_FILE_NAME, of(84)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSubflow() throws Exception {
    flowRunner("flowWithSubflow").run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SUBFLOW.appendProcessorsPart();
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(88)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", FLOW_REF, CONFIG_FILE_NAME, of(89)));

    assertNextProcessorLocationIs(SUBFLOW.appendProcessorsPart()
        .appendLocationPart("0", LOGGER,
                            CONFIG_FILE_NAME, of(94)));
    assertNextProcessorLocationIs(SUBFLOW.appendProcessorsPart()
        .appendLocationPart("1", VALIDATION_IS_TRUE,
                            CONFIG_FILE_NAME, of(95)));

    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(90)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithScatterGather() throws Exception {
    flowRunner("flowWithScatterGather").run();
    waitUntilNotificationsArrived(4);
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SCATTER_GATHER.appendLocationPart("processors", empty(), empty(), empty());
    DefaultComponentLocation scatterGatherLocation =
        flowWithSplitterProcessorsLocation.appendLocationPart("0", SCATTER_GATHER, CONFIG_FILE_NAME, of(99));
    assertNextProcessorLocationIs(scatterGatherLocation);
    DefaultComponentLocation scatterGatherRoute0 = scatterGatherLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(100));
    assertNextProcessorLocationIs(scatterGatherRoute0
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(101)));
    DefaultComponentLocation scatterGatherRouter1 = scatterGatherLocation
        .appendRoutePart()
        .appendLocationPart("1", ROUTE, CONFIG_FILE_NAME, of(103));
    assertNextProcessorLocationIs(scatterGatherRouter1
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(104)));
    DefaultComponentLocation scatterGatherRouter2 = scatterGatherLocation.appendRoutePart()
        .appendLocationPart("2", ROUTE, CONFIG_FILE_NAME, of(106));
    assertNextProcessorLocationIs(scatterGatherRouter2
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME, of(107)));
    assertNoNextProcessorNotification();
  }

  @Test
  @Ignore("MULE-13456")
  public void flowWithAsync() throws Exception {
    flowRunner("flowWithAsync").run();
    waitUntilNotificationsArrived(3);
    DefaultComponentLocation flowWithAsyncLocation = FLOW_WITH_ASYNC.appendProcessorsPart();
    DefaultComponentLocation asyncLocation = flowWithAsyncLocation
        .appendLocationPart("0", ASYNC, CONFIG_FILE_NAME, of(113));
    assertNextProcessorLocationIs(asyncLocation);
    DefaultComponentLocation asyncProcessorsLocation = asyncLocation
        .appendProcessorsPart();
    assertNextProcessorLocationIs(asyncProcessorsLocation
        .appendLocationPart("0", LOGGER,
                            CONFIG_FILE_NAME, of(114)));
    assertNextProcessorLocationIs(asyncProcessorsLocation
        .appendLocationPart("1", VALIDATION_IS_TRUE,
                            CONFIG_FILE_NAME, of(115)));
    assertNoNextProcessorNotification();
  }

  private void waitUntilNotificationsArrived(int minimumRequiredNotifications) {
    new PollingProber(RECEIVE_TIMEOUT, 100).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return getNotificationsStore().getNotifications().size() >= minimumRequiredNotifications;
      }

      @Override
      public String describeFailure() {
        return "not all expected notifications arrived";
      }
    });
  }

  private void assertNoNextProcessorNotification() {
    ProcessorNotificationStore processorNotificationStore = getNotificationsStore();
    Iterator iterator = processorNotificationStore.getNotifications().iterator();
    assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    ProcessorNotificationStore processorNotificationStore = getNotificationsStore();
    assertThat(processorNotificationStore.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        processorNotificationStore.getNotifications().get(0);
    processorNotificationStore.getNotifications().remove(0);
    assertThat(processorNotification.getComponent().getLocation().getLocation(), is(componentLocation.getLocation()));
    assertThat(processorNotification.getComponent().getLocation(), is(componentLocation));
  }

  private ProcessorNotificationStore getNotificationsStore() {
    return registry.<ProcessorNotificationStore>lookupByName("notificationsStore").get();
  }

}
