/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

module org.mule.tests.integration {

  requires org.mule.runtime.api;
  requires org.mule.runtime.http.api;
  requires org.mule.runtime.core;

  requires org.mule.test.runner;
  requires org.mule.test.allure;
  requires test.components;

  requires org.hamcrest;
  requires junit;
  requires io.qameta.allure.commons;
  
}