/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

module org.mule.test.tooling.support {

  requires org.mule.oauth.client.api;
  requires org.mule.sdk.api;
  requires org.mule.sdk.compatibility.api;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.http.api;
  requires org.mule.runtime.launcher;
  requires org.mule.runtime.maven.client.test;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.oauth.api;
  requires org.mule.runtime.tooling.support;
  requires org.mule.weave.mule.dwb.api;

  requires com.google.common;
  requires org.apache.logging.log4j;
  requires java.sql;

  exports org.mule.runtime.module.tooling.test;
}