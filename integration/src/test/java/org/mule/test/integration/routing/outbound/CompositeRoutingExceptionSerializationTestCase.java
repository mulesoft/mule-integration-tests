/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.privileged.routing.CompositeRoutingException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.io.InputStream;

import javax.inject.Inject;

import io.qameta.allure.Issue;
import org.junit.Test;

/**
 * Integration test for verifying that CompositeRoutingException is serializable using the default Java serializer.
 */
public class CompositeRoutingExceptionSerializationTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Inject
  private ObjectSerializer defaultSerializer;

  private static final String JAVA_FILENAME =
      "CompositeRoutingExceptionSerializationTestCase_testCanDeserializeCompositeRouterExceptionFromLegacyVersion.ser";

  private void verifyDeserializedError(Error deserializedError) {
    // Assert that the deserialized error is an instance of CompositeRoutingException
    assertThat(deserializedError.getCause(), instanceOf(CompositeRoutingException.class));

    // Assert that the deserialized error description contains the inner errors descriptions
    assertThat(deserializedError.getDescription(), containsString("Route 0 failed - 474bab53-1135-4038-a894-b6ba646b3f00"));
    assertThat(deserializedError.getDescription(), containsString("Route 1 failed - 541ad1d6-0fc2-44d2-8b98-b8d71b9a5f66"));

    // Assert that the inner errors are present in the CompositeRoutingException
    CompositeRoutingException ex = (CompositeRoutingException) deserializedError.getCause();
    assertThat(ex.getErrors().get(0).getErrorType().toString(), equalTo("CUSTOM:ROUTE0"));
    assertThat(ex.getErrors().get(1).getErrorType().toString(), equalTo("CUSTOM:ROUTE1"));
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/composite-routing-exception.xml";
  }

  @Test
  @Issue("W-16562974")
  public void testJavaCanSerializeAndDeserializeCompositeRouterException() throws Exception {
    // Run the flow that throws CompositeRoutingException and push the error to the dead letter queue
    flowRunner("compositeRoutingErrorFlow").run();

    // Read the error from the dead letter queue
    Error error = (Error) queueManager.read("dlq", RECEIVE_TIMEOUT, MILLISECONDS).getMessage().getPayload().getValue();

    // Serialize and deserialize the error
    byte[] errorSerialization = defaultSerializer.getInternalProtocol().serialize(error);
    Error deserializedError = defaultSerializer.getInternalProtocol().deserialize(errorSerialization);

    // Verify the deserialized error content
    verifyDeserializedError(deserializedError);
  }

  @Test
  @Issue("W-16562974")
  public void testJavaCanDeserializeCompositeRouterExceptionFromLegacyVersion() throws Exception {
    // Deserialize the legacy error
    Error deserializedError;
    try (InputStream errorSerializationInputStream = getClass().getResourceAsStream(JAVA_FILENAME)) {
      deserializedError = defaultSerializer.getInternalProtocol().deserialize(errorSerializationInputStream);
    }

    // Verify the deserialized error content
    verifyDeserializedError(deserializedError);
  }
}
