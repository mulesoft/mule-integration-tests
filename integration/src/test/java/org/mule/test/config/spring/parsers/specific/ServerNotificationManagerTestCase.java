/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.specific;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.context.notification.SecurityNotification;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;

import org.junit.Test;

public class ServerNotificationManagerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/specific/server-notification-manager-test.xml";
  }

  @Test
  public void testDynamicAttribute() {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertThat(manager.isNotificationDynamic(), is(true));
  }

  @Test
  public void testRoutingConfiguration() {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertThat(manager.getInterfaceToTypes().entrySet(), hasSize(greaterThan(2)));
    Object ifaces = manager.getInterfaceToTypes().get(TestInterface.class);
    assertThat(ifaces, not(nullValue()));
    assertThat(ifaces, instanceOf(Collection.class));
    assertThat((Collection<Class>) ifaces, hasItem(TestEvent.class));
    ifaces = manager.getInterfaceToTypes().get(TestInterface2.class);
    assertThat(ifaces, not(nullValue()));
    assertThat(ifaces, instanceOf(Collection.class));
    assertThat((Collection<Class>) ifaces, hasItem(SecurityNotification.class));
  }

  @Test
  public void testSimpleNotification() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    Collection<ListenerSubscriptionPair> listeners = manager.getListeners();
    // Now all transformers are registered as listeners in order to get a context disposing notification
    assertThat(listeners, hasSize(greaterThan(5)));
    TestListener listener = (TestListener) muleContext.getRegistry().lookupObject("listener");
    assertThat(listener, not(nullValue()));
    assertThat(listener.isCalled(), is(false));
    manager.fireNotification(new TestEvent());

    // asynch events
    new PollingProber(1000, 50).check(new JUnitLambdaProbe(() -> listener.isCalled()));
  }

  @Test
  public void testExplicitlyConiguredNotificationListenerRegistration() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    assertThat(manager.getListeners(),
               hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("listener"), null)));
    assertThat(manager.getListeners(),
               hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("listener2"), null)));
    assertThat(manager.getListeners(),
               hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("securityListener"), null)));
    assertThat(manager.getListeners(),
               hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("listener3"), "*")));
  }

  @Test
  public void testAdhocNotificationListenerRegistrations() throws InterruptedException {
    ServerNotificationManager manager = muleContext.getNotificationManager();

    // Not registered as ad-hoc listener with null subscription as this is defined
    // explicitly.
    assertThat(manager.getListeners(),
               not(hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("listener3"), null))));

    // Registered as configured
    assertThat(manager.getListeners(),
               hasItem(new ListenerSubscriptionPair((ServerNotificationListener) muleContext.getRegistry()
                   .lookupObject("listener4"), null)));
  }

  @Test
  public void testDisabledNotification() throws Exception {
    ServerNotificationManager manager = muleContext.getNotificationManager();
    Collection<ListenerSubscriptionPair> listeners = manager.getListeners();
    // Now all transformers are registered as listeners in order to get a context disposing notification
    assertThat(listeners, hasSize(greaterThan(5)));
    TestListener2 listener2 = muleContext.getRegistry().lookupObject("listener2");
    assertThat(listener2, not(nullValue()));
    assertThat(listener2.isCalled(), is(false));
    TestSecurityListener adminListener = muleContext.getRegistry().lookupObject("securityListener");
    assertThat(adminListener, not(nullValue()));
    assertThat(adminListener.isCalled(), is(false));
    manager.fireNotification(new TestSecurityEvent(muleContext));
    new PollingProber(2000, 100).check(new JUnitLambdaProbe(() -> listener2.isCalled(), "listener2 should be notified"));
    assertThat(adminListener.isCalled(), is(false));
  }

  protected static interface TestInterface extends ServerNotificationListener {
    // empty
  }

  protected static interface TestInterface2 extends ServerNotificationListener {
    // empty
  }

  protected static class TestListener implements TestInterface {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(ServerNotification notification) {
      called = true;
    }

  }

  protected static class TestListener2 implements TestInterface2 {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public void onNotification(ServerNotification notification) {
      called = true;
    }

  }

  protected static class TestSecurityListener implements SecurityNotificationListener<SecurityNotification> {

    private boolean called = false;

    public boolean isCalled() {
      return called;
    }

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    public void onNotification(SecurityNotification notification) {
      called = true;
    }

  }

  protected static class TestEvent extends ServerNotification {

    public TestEvent() {
      super(new Object(), 0);
    }

  }

  protected static class TestSecurityEvent extends SecurityNotification {

    public TestSecurityEvent(MuleContext muleContext) throws Exception {
      super(new UnauthorisedException(createStaticMessage("dummy")), 0);
    }

  }

}
