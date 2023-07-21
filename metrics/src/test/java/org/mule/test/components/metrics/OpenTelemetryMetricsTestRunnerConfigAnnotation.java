/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.metrics;


import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    applicationRuntimeLibs = {
        "org.apache.commons:commons-lang3"
    },
    extraPrivilegedArtifacts = {
        "org.mule.tests:mule-tests-parsers-plugin"
    })
public interface OpenTelemetryMetricsTestRunnerConfigAnnotation {
}
