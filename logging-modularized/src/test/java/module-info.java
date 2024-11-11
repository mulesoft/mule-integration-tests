/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Logging Module.
 *
 * @moduleGraph
 * @since 4.7
 */
module org.mule.runtime.test.integration.logging.modularized {

  // Mule modules
  requires org.mule.runtime.boot.log4j;
  requires org.mule.runtime.core;
  requires org.mule.runtime.log4j;
  requires org.mule.test.allure;
  requires org.mule.test.infrastructure;
  requires org.mule.test.unit;

  // Log bridges
  requires jul.to.slf4j;
//  requires org.apache.log4j;

}
