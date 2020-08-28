/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.notification.PipelineMessageNotificationListener;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;

public class RedeliveryPolicyWithInterceptorsTestCase extends AbstractIntegrationTestCase {

  private static final String REDELIVERY_POLICY_FLOW_DISPATCH_FLOW = "redeliveryPolicyFlowDispatch";
  private static final String REDELIVERY_POLICY_FLOW_PROCESS_FLOW = "redeliveryPolicyFlowProcess";
  private static final String REDELIVERY_POLICY_COMPONENT = "redelivery-policy";
  private static final String REDELIVERED_MESSAGE_QUEUE = "redeliveredMessageQueue";

  private static CountDownLatch latch;
  private static AtomicInteger awaiting = new AtomicInteger();
  private static AtomicBoolean pipelineCompleted = new AtomicBoolean(false);
  private static boolean exceptionInterceptedAfterPipelineCompleted = false;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/redelivery-policy-with-interceptors-config.xml";
  }

  @Test
  @Issue("MULE-18707")
  public void noRedeliveryPolicyExceptionInterceptedAfterPipelineCompleted() throws Exception {
    configNotificationListener();

    flowRunner(REDELIVERY_POLICY_FLOW_DISPATCH_FLOW)
        .withPayload("{ \"name\" : \"bruce\"}")
        .withMediaType(APPLICATION_JSON)
        .runExpectingException();

    assertThat(exceptionInterceptedAfterPipelineCompleted, is(FALSE));
  }

  private void configNotificationListener() {
    muleContext.getNotificationManager().addListener(new PipelineMessageNotificationListener<PipelineMessageNotification>() {

      @Override
      public void onNotification(PipelineMessageNotification notification) {
        if (isRedeliveryFlowProcessCompleted(notification)) {
          pipelineCompleted.set(true);
        }
      }

      private boolean isRedeliveryFlowProcessCompleted(PipelineMessageNotification notification) {
        return notification.getAction().getActionId() == PROCESS_COMPLETE
            && notification.getResourceIdentifier().equals(REDELIVERY_POLICY_FLOW_PROCESS_FLOW);
      }

    });
  }

  public static class TestInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new ProcessorInterceptor() {

        @Override
        public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
          if (pipelineCompleted.get()
              && location.getComponentIdentifier().getIdentifier().getName().equals(REDELIVERY_POLICY_COMPONENT)) {
            exceptionInterceptedAfterPipelineCompleted = true;
          }
        }
      };
    }
  }

}
