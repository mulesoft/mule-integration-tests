/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
