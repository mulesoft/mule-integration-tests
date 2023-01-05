/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.OptionalInt.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SUBFLOW_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocationStory.COMPONENT_LOCATION;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(COMPONENT_LOCATION)
public class ComponentLocationTestCase extends AbstractIntegrationTestCase {

  private static final Optional<TypedComponentIdentifier> FLOW_TYPED_COMPONENT_IDENTIFIER =
      Optional.of(builder().identifier(FLOW_IDENTIFIER).type(FLOW).build());

  private static final Optional<TypedComponentIdentifier> SUB_FLOW_TYPED_COMPONENT_IDENTIFIER =
      Optional.of(builder().identifier(SUBFLOW_IDENTIFIER).type(SCOPE).build());

  private static final Optional<String> CONFIG_FILE_NAME =
      Optional.of("org/mule/test/integration/notifications/component-path-test-flow.xml");

  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      new DefaultComponentLocation(Optional.of("flowWithSingleMp"),
                                   asList(new DefaultLocationPart("flowWithSingleMp",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(27),
                                                                  of(5))));
  private static final DefaultComponentLocation FLOW_WITH_MULTIPLE_MP_LOCATION =
      new DefaultComponentLocation(Optional.of("flowWithMultipleMps"),
                                   asList(new DefaultLocationPart("flowWithMultipleMps",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(31),
                                                                  of(5))));
  private static final DefaultComponentLocation FLOW_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(Optional.of("flowWithErrorHandler"),
                                   asList(new DefaultLocationPart("flowWithErrorHandler",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(37),
                                                                  of(5))));
  private static final DefaultComponentLocation FLOW_WITH_BLOCK_WITH_ERROR_HANDLER =
      new DefaultComponentLocation(Optional.of("flowWithTryWithErrorHandler"),
                                   asList(new DefaultLocationPart("flowWithTryWithErrorHandler",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(58),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_SOURCE =
      new DefaultComponentLocation(Optional.of("flowWithSource"),
                                   asList(new DefaultLocationPart("flowWithSource",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(70),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_SCATTER_GATHER =
      new DefaultComponentLocation(Optional.of("flowWithScatterGather"),
                                   asList(new DefaultLocationPart("flowWithScatterGather",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(90),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_ASYNC =
      new DefaultComponentLocation(Optional.of("flowWithAsync"),
                                   asList(new DefaultLocationPart("flowWithAsync",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(104),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_SUBFLOW =
      new DefaultComponentLocation(Optional.of("flowWithSubflow"),
                                   asList(new DefaultLocationPart("flowWithSubflow",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(79),
                                                                  of(5))));


  private static final DefaultComponentLocation SUBFLOW =
      new DefaultComponentLocation(Optional.of("subflow"),
                                   asList(new DefaultLocationPart("subflow",
                                                                  SUB_FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(85),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_AGGREGATOR_ONE_ROUTE =
      new DefaultComponentLocation(Optional.of("aggregatorWithOneRoute"),
                                   asList(new DefaultLocationPart("aggregatorWithOneRoute",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(117),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_AGGREGATOR_TWO_ROUTES =
      new DefaultComponentLocation(Optional.of("aggregatorWithTwoRoutes"),
                                   asList(new DefaultLocationPart("aggregatorWithTwoRoutes",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(125),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_AGGREGATOR_TWO_ROUTES_AND_CONTENT =
      new DefaultComponentLocation(Optional.of("aggregatorWithTwoRoutesAndContent"),
                                   asList(new DefaultLocationPart("aggregatorWithTwoRoutesAndContent",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(136),
                                                                  of(5))));

  private static final DefaultComponentLocation FLOW_WITH_OPERATION_WITH_CHAIN =
      new DefaultComponentLocation(Optional.of("operationWithChain"),
                                   asList(new DefaultLocationPart("operationWithChain",
                                                                  FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                  CONFIG_FILE_NAME,
                                                                  of(148),
                                                                  of(5))));

  private static final Optional<TypedComponentIdentifier> LOGGER =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:logger"))
          .type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> SET_PAYLOAD =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:set-payload"))
          .type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> OBJECT_TO_STRING_TRANSFORMER =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:object-to-byte-array-transformer")).type(UNKNOWN)
          .build());
  private static final Optional<TypedComponentIdentifier> CHOICE =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:choice")).type(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> ERROR_HANDLER =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:error-handler"))
          .type(TypedComponentIdentifier.ComponentType.ERROR_HANDLER).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_CONTINUE =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:on-error-continue")).type(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_FALSE =
      Optional.of(builder().identifier(buildFromStringRepresentation("validation:is-false")).type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> TEST_PROCESSOR =
      Optional.of(builder().identifier(buildFromStringRepresentation("test:processor")).type(UNKNOWN).build());
  private static final Optional<TypedComponentIdentifier> ON_ERROR_PROPAGATE =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:on-error-propagate")).type(ON_ERROR).build());
  private static final Optional<TypedComponentIdentifier> TRY =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:try")).type(SCOPE).build());
  private static final Optional<TypedComponentIdentifier> VALIDATION_IS_TRUE =
      Optional.of(builder().identifier(buildFromStringRepresentation("validation:is-true")).type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> SCHEDULER_SOURCE =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:scheduler")).type(SOURCE).build());
  private static final Optional<TypedComponentIdentifier> SCATTER_GATHER =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:scatter-gather")).type(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> FLOW_REF =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:flow-ref")).type(OPERATION).build());
  private static final Optional<TypedComponentIdentifier> ASYNC =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:async")).type(SCOPE).build());
  private static final Optional<TypedComponentIdentifier> ROUTE =
      Optional.of(builder().identifier(buildFromStringRepresentation("mule:route")).type(SCOPE).build());

  private static final Optional<TypedComponentIdentifier> AGGREGATOR =
      Optional.of(builder().identifier(buildFromStringRepresentation("aggregators:size-based-aggregator")).type(ROUTER).build());
  private static final Optional<TypedComponentIdentifier> TAP_PHONES =
      Optional.of(builder().identifier(buildFromStringRepresentation("heisenberg:tap-phones")).type(OPERATION).build());


  @Inject
  @Named("flowWithSource")
  private Flow flowWithSource;

  @Inject
  private ConfigurationComponentLocator configurationComponentLocator;

  @Override
  protected String getConfigFile() {
    return CONFIG_FILE_NAME.get();
  }

  @After
  public void clearNotifications() {
    final ProcessorNotificationStore notificationsStore = getNotificationsStore();
    if (notificationsStore != null) {
      notificationsStore.getNotifications().clear();
    }
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(28), of(9)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithMultipleMps() throws Exception {
    flowRunner("flowWithMultipleMps").run();
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(32), of(9)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
        .appendLocationPart("1", SET_PAYLOAD, CONFIG_FILE_NAME, of(33), of(9)));
    assertNextProcessorLocationIs(FLOW_WITH_MULTIPLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
        .appendLocationPart("2", OBJECT_TO_STRING_TRANSFORMER,
                            CONFIG_FILE_NAME, of(34), of(9)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnContinue() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", false).run();
    DefaultComponentLocation choiceLocation =
        FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
            .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38), of(9));
    assertNextProcessorLocationIs(choiceLocation);
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER,
                            CONFIG_FILE_NAME, of(46), of(9))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(47), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(48), of(17)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithErrorHandlerExecutingOnPropagate() throws Exception {
    flowRunner("flowWithErrorHandler").withVariable("executeFailingComponent", true).runExpectingException();
    DefaultComponentLocation choiceLocation =
        FLOW_WITH_ERROR_HANDLER.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
            .appendLocationPart("0", CHOICE, CONFIG_FILE_NAME, of(38), of(9));
    assertNextProcessorLocationIs(choiceLocation);
    DefaultComponentLocation choiceRoute0 = choiceLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(39), of(13));
    assertNextProcessorLocationIs(choiceRoute0
        .appendProcessorsPart()
        .appendLocationPart("0", TEST_PROCESSOR, CONFIG_FILE_NAME, of(40), of(17)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46), of(9))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME, of(50), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(51), of(17)));
    assertNextProcessorLocationIs(FLOW_WITH_ERROR_HANDLER
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(46), of(9))
        .appendLocationPart("1", ON_ERROR_PROPAGATE, CONFIG_FILE_NAME,
                            of(50), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(51), of(17))
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(52), of(21)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithBlockWithErrorHandler() throws Exception {
    flowRunner("flowWithTryWithErrorHandler").run();
    DefaultComponentLocation blockLocation =
        FLOW_WITH_BLOCK_WITH_ERROR_HANDLER
            .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
            .appendLocationPart("0", TRY, CONFIG_FILE_NAME, of(59), of(9));
    assertNextProcessorLocationIs(blockLocation);
    assertNextProcessorLocationIs(blockLocation
        .appendProcessorsPart()
        .appendLocationPart("0", TEST_PROCESSOR, CONFIG_FILE_NAME, of(60), of(13)));
    DefaultComponentLocation blockOnErrorContinueLocation = blockLocation
        .appendLocationPart("errorHandler", ERROR_HANDLER, CONFIG_FILE_NAME, of(61), of(13))
        .appendLocationPart("0", ON_ERROR_CONTINUE, CONFIG_FILE_NAME,
                            of(62), of(17));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendProcessorsPart()
        .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME, of(63), of(21)));
    assertNextProcessorLocationIs(blockOnErrorContinueLocation
        .appendProcessorsPart()
        .appendLocationPart("1", VALIDATION_IS_TRUE, CONFIG_FILE_NAME, of(64), of(21)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSource() {
    DefaultComponentLocation expectedSourceLocation =
        FLOW_WITH_SOURCE.appendLocationPart("source", SCHEDULER_SOURCE,
                                            CONFIG_FILE_NAME, of(71), of(9));
    DefaultComponentLocation sourceLocation =
        (DefaultComponentLocation) (flowWithSource.getSource()).getAnnotation(LOCATION_KEY);
    assertThat(sourceLocation, is(expectedSourceLocation));
    assertThat(((Component) flowWithSource.getProcessors().get(0)).getAnnotation(LOCATION_KEY), is(FLOW_WITH_SOURCE
        .appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(76), of(9))));
  }

  @Test
  public void flowWithSubflow() throws Exception {
    flowRunner("flowWithSubflow").run();
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SUBFLOW.appendProcessorsPart();
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(80), of(9)));
    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("1", FLOW_REF, CONFIG_FILE_NAME, of(81), of(9)));

    assertNextProcessorLocationIs(SUBFLOW.appendProcessorsPart()
        .appendLocationPart("0", LOGGER,
                            CONFIG_FILE_NAME, of(86), of(9)));
    assertNextProcessorLocationIs(SUBFLOW.appendProcessorsPart()
        .appendLocationPart("1", VALIDATION_IS_TRUE,
                            CONFIG_FILE_NAME, of(87), of(9)));

    assertNextProcessorLocationIs(flowWithSplitterProcessorsLocation
        .appendLocationPart("2", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                            of(82), of(9)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithScatterGather() throws Exception {
    flowRunner("flowWithScatterGather").run();
    waitUntilNotificationsArrived(4);
    DefaultComponentLocation flowWithSplitterProcessorsLocation =
        FLOW_WITH_SCATTER_GATHER.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty());
    DefaultComponentLocation scatterGatherLocation =
        flowWithSplitterProcessorsLocation.appendLocationPart("0", SCATTER_GATHER, CONFIG_FILE_NAME, of(91), of(9));
    assertNextProcessorLocationIs(scatterGatherLocation);

    DefaultComponentLocation scatterGatherRoute0 = scatterGatherLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(92), of(13));
    DefaultComponentLocation scatterGatherRouter1 = scatterGatherLocation
        .appendRoutePart()
        .appendLocationPart("1", ROUTE, CONFIG_FILE_NAME, of(95), of(13));
    DefaultComponentLocation scatterGatherRouter2 = scatterGatherLocation
        .appendRoutePart()
        .appendLocationPart("2", ROUTE, CONFIG_FILE_NAME, of(98), of(13));

    List<DefaultComponentLocation> nextLocations = asList(
                                                          scatterGatherRoute0
                                                              .appendProcessorsPart()
                                                              .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME,
                                                                                  of(93), of(17)),
                                                          scatterGatherRouter1
                                                              .appendProcessorsPart()
                                                              .appendLocationPart("0", VALIDATION_IS_TRUE, CONFIG_FILE_NAME,
                                                                                  of(96), of(17)),
                                                          scatterGatherRouter2
                                                              .appendProcessorsPart()
                                                              .appendLocationPart("0", VALIDATION_IS_FALSE, CONFIG_FILE_NAME,
                                                                                  of(99), of(17)));

    assertNextProcessorLocationsAre(nextLocations);
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithAsync() throws Exception {
    FlowRunner runner = flowRunner("flowWithAsync");
    CountDownLatch terminationLatch = new CountDownLatch(1);
    ((BaseEventContext) (runner.buildEvent().getContext())).onTerminated((e, t) -> terminationLatch.countDown());

    runner.run();
    waitUntilNotificationsArrived(3);
    DefaultComponentLocation flowWithAsyncLocation = FLOW_WITH_ASYNC.appendProcessorsPart();
    DefaultComponentLocation asyncLocation = flowWithAsyncLocation
        .appendLocationPart("0", ASYNC, CONFIG_FILE_NAME, of(105), of(9));
    assertNextProcessorLocationIs(asyncLocation);
    DefaultComponentLocation asyncProcessorsLocation = asyncLocation
        .appendProcessorsPart();
    assertNextProcessorLocationIs(asyncProcessorsLocation
        .appendLocationPart("0", LOGGER,
                            CONFIG_FILE_NAME, of(106), of(13)));
    assertNextProcessorLocationIs(asyncProcessorsLocation
        .appendLocationPart("1", VALIDATION_IS_TRUE,
                            CONFIG_FILE_NAME, of(107), of(13)));
    assertNoNextProcessorNotification();

    terminationLatch.await();
  }

  @Test
  public void defaultErrorHandlerFromFlowCannotBeAccesed() throws Exception {
    Location defaultErrorHandlerLoggerLocation = Location.builder().globalName("flowWithSingleMp").addErrorHandlerPart()
        .addIndexPart(0).addProcessorsPart().addIndexPart(0).build();
    Optional<Component> component = configurationComponentLocator.find(defaultErrorHandlerLoggerLocation);
    assertThat(component.isPresent(), is(false));
  }

  @Test
  public void defaultErrorHandler() throws Exception {
    Location defaultErrorHandlerLoggerLocation = Location.builder().globalName("defaultErrorHandler").build();
    Optional<Component> component = configurationComponentLocator.find(defaultErrorHandlerLoggerLocation);
    assertThat(component.isPresent(), is(true));
  }

  @Test
  public void aggregatorWithOneRoute() throws Exception {
    flowRunner("aggregatorWithOneRoute").run();
    waitUntilNotificationsArrived(2);
    DefaultComponentLocation flowWithSingleRouteAggregator =
        FLOW_WITH_AGGREGATOR_ONE_ROUTE.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(),
                                                          OptionalInt.empty());
    DefaultComponentLocation aggregatorLocation =
        flowWithSingleRouteAggregator.appendLocationPart("0", AGGREGATOR, CONFIG_FILE_NAME, of(118), of(9));
    assertNextProcessorLocationIs(aggregatorLocation);

    DefaultComponentLocation aggregatorRoute0 = aggregatorLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(119), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(120), of(17));


    assertNextProcessorLocationIs(aggregatorRoute0);

    assertNoNextProcessorNotification();
  }

  @Test
  public void aggregatorWithTwoRoutes() throws Exception {
    final String flowName = "aggregatorWithTwoRoutes";
    flowRunner(flowName).run();
    waitUntilNotificationsArrived(2);
    DefaultComponentLocation flowWithSingleRouteAggregator =
        FLOW_WITH_AGGREGATOR_TWO_ROUTES.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(),
                                                           OptionalInt.empty());
    DefaultComponentLocation aggregatorLocation =
        flowWithSingleRouteAggregator.appendLocationPart("0", AGGREGATOR, CONFIG_FILE_NAME, of(126), of(9));
    assertNextProcessorLocationIs(aggregatorLocation);

    DefaultComponentLocation aggregatorRoute0 = aggregatorLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(127), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(128), of(17));

    assertNextProcessorLocationIs(aggregatorRoute0);

    flowRunner(flowName).run();
    waitUntilNotificationsArrived(2);

    assertNextProcessorLocationIs(aggregatorLocation);

    DefaultComponentLocation aggregatorRoute1 = aggregatorLocation
        .appendRoutePart()
        .appendLocationPart("1", ROUTE, CONFIG_FILE_NAME, of(130), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(131), of(17));

    assertNextProcessorLocationIs(aggregatorRoute1);

    assertNoNextProcessorNotification();
  }

  @Test
  public void aggregatorWithTwoRoutesAndContent() throws Exception {
    final String flowName = "aggregatorWithTwoRoutesAndContent";
    flowRunner(flowName).run();
    waitUntilNotificationsArrived(2);
    DefaultComponentLocation flowWithSingleRouteAggregator =
        FLOW_WITH_AGGREGATOR_TWO_ROUTES_AND_CONTENT.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(),
                                                                       OptionalInt.empty());
    DefaultComponentLocation aggregatorLocation =
        flowWithSingleRouteAggregator.appendLocationPart("0", AGGREGATOR, CONFIG_FILE_NAME, of(137), of(9));
    assertNextProcessorLocationIs(aggregatorLocation);

    DefaultComponentLocation aggregatorRoute0 = aggregatorLocation
        .appendRoutePart()
        .appendLocationPart("0", ROUTE, CONFIG_FILE_NAME, of(139), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(140), of(17));

    assertNextProcessorLocationIs(aggregatorRoute0);

    flowRunner(flowName).run();
    waitUntilNotificationsArrived(2);

    assertNextProcessorLocationIs(aggregatorLocation);

    DefaultComponentLocation aggregatorRoute1 = aggregatorLocation
        .appendRoutePart()
        .appendLocationPart("1", ROUTE, CONFIG_FILE_NAME, of(142), of(13))
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(143), of(17));

    assertNextProcessorLocationIs(aggregatorRoute1);

    assertNoNextProcessorNotification();
  }

  @Test
  @Issue("MULE-18504")
  public void operationWithChain() throws Exception {
    final String flowName = "operationWithChain";
    flowRunner(flowName).run();
    waitUntilNotificationsArrived(2);
    DefaultComponentLocation operationWithChain =
        FLOW_WITH_OPERATION_WITH_CHAIN.appendLocationPart("processors", empty(), empty(), OptionalInt.empty(),
                                                          OptionalInt.empty());
    DefaultComponentLocation operationWithChainLocation =
        operationWithChain.appendLocationPart("0", TAP_PHONES, CONFIG_FILE_NAME, of(149), of(9));
    assertNextProcessorLocationIs(operationWithChainLocation);

    DefaultComponentLocation innerChainRoute = operationWithChainLocation
        .appendProcessorsPart()
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(150), of(13));

    assertNextProcessorLocationIs(innerChainRoute);
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
    Iterator<MessageProcessorNotification> iterator = processorNotificationStore.getNotifications().iterator();
    assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    ProcessorNotificationStore processorNotificationStore = getNotificationsStore();
    assertThat(processorNotificationStore.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        processorNotificationStore.getNotifications().get(0);
    assertThat(processorNotification.getComponent().getLocation().getLocation(), is(componentLocation.getLocation()));
    assertThat(processorNotification.getComponent().getLocation(), is(componentLocation));
    processorNotificationStore.getNotifications().remove(0);
  }

  // Check in any order
  private void assertNextProcessorLocationsAre(List<DefaultComponentLocation> componentLocations) {
    StringBuilder errors = new StringBuilder();
    for (int i = 0; i < componentLocations.size(); i++) {
      for (DefaultComponentLocation componentLocation : componentLocations) {
        try {
          assertNextProcessorLocationIs(componentLocation);
          errors = new StringBuilder();
          break;
        } catch (AssertionError e) {
          errors.append(e.getMessage());
        }
      }
      String errorString = errors.toString();
      if (!errorString.isEmpty()) {
        fail(format("Not every componentLocation was found in the notification list. %s", errorString));
      }
    }
  }

  private ProcessorNotificationStore getNotificationsStore() {
    return registry.<ProcessorNotificationStore>lookupByName("notificationsStore").get();
  }

}
