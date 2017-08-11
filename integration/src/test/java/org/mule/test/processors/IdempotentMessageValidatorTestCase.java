/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IdempotentMessageValidatorTestCase extends AbstractIntegrationTestCase {


  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  public String getConfigFile() {
    return "org/mule/processors/idempotent-message-validator-config.xml";
  }


  @Test
  public void validateWithGlobalObjectStore() throws Exception {
    String payload = "payload";
    InternalEvent response = flowRunner("validate-global").withPayload(payload).run();
    expectedException.expect(MessagingException.class);
    flowRunner("validate-global").withPayload(payload).run();
    assertThat(response.getMessage().getPayload().getValue(), is(equalTo(payload)));
  }

  @Test
  public void validateWithGlobalObjectStoreFromDifferentValidators() throws Exception {
    String payload = "payload";
    InternalEvent response = flowRunner("validate-global").withPayload(payload).run();
    expectedException.expect(MessagingException.class);
    flowRunner("validate-global2").withPayload(payload).run();
    assertThat(response.getMessage().getPayload().getValue(), is(equalTo(payload)));
  }

  @Test
  public void validateWithPrivateObjectStoreUsesADifferentObjectStore() throws Exception {
    String globalPayload = "global-payload";
    String privatePayload = "private-payload";
    InternalEvent globalResponse = flowRunner("validate-global").withPayload(globalPayload).run();
    InternalEvent privateResponse = flowRunner("validate-private").withPayload(privatePayload).run();
    try {
      flowRunner("validate-global").withPayload(globalPayload).run();
    } catch (MessagingException e) {
      expectedException.expect(MessagingException.class);
      flowRunner("validate-private").withPayload(privatePayload).run();
    }
    assertThat(globalResponse.getMessage().getPayload().getValue(), is(equalTo(globalPayload)));
    assertThat(privateResponse.getMessage().getPayload().getValue(), is(equalTo(privatePayload)));
  }

  @Test
  public void validateWithImplicitObjectStoreUsesADifferentObjectStore() throws Exception {
    String globalPayload = "global-payload";
    String implicitPayload = "implicit-payload";
    InternalEvent globalResponse = flowRunner("validate-global").withPayload(globalPayload).run();
    InternalEvent implicitResponse = flowRunner("validate-implicit").withPayload(implicitPayload).run();
    try {
      flowRunner("validate-global").withPayload(globalPayload).run();
    } catch (MessagingException e) {
      expectedException.expect(MessagingException.class);
      flowRunner("validate-implicit").withPayload(implicitPayload).run();
    }
    assertThat(globalResponse.getMessage().getPayload().getValue(), is(equalTo(globalPayload)));
    assertThat(implicitResponse.getMessage().getPayload().getValue(), is(equalTo(implicitPayload)));
  }

}
