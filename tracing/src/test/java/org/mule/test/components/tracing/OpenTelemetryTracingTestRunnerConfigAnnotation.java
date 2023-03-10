/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    applicationRuntimeLibs = {
        "org.apache.commons:commons-lang3"
    },
    applicationSharedRuntimeLibs = {
        "org.apache.activemq:activemq-broker",
        "org.apache.activemq:activemq-client",
        "org.apache.activemq:activemq-kahadb-store",
        "org.fusesource.hawtbuf:hawtbuf",
        "org.apache.activemq.protobuf:activemq-protobuf",
        "org.mule.tests:mule-tests-model",
        "org.mule.runtime:mule-tracer-exporter-impl"
    },
    extraPrivilegedArtifacts = {
        "org.mule.tests:mule-tests-parsers-plugin"
    },
    testRunnerExportedRuntimeLibs = {
        "org.mule.tests:mule-tests-functional",
        "io.opentelemetry:opentelemetry-api",
        "io.opentelemetry:opentelemetry-sdk",
        "io.opentelemetry:opentelemetry-sdk-common",
        "io.opentelemetry:opentelemetry-sdk-trace",
        "io.opentelemetry:opentelemetry-sdk-metrics",
        "io.opentelemetry:opentelemetry-context"
    })
public interface OpenTelemetryTracingTestRunnerConfigAnnotation {

}
