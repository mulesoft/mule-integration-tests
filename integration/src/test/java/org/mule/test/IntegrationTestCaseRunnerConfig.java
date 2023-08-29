/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    applicationRuntimeLibs = {"org.apache.commons:commons-lang3"},
    applicationSharedRuntimeLibs = {
        "org.mule.tests:mule-activemq-broker",
        "com.fasterxml.jackson.core:jackson-core",
        "com.fasterxml.jackson.core:jackson-annotations",
        "com.fasterxml.jackson.core:jackson-databind",
        "org.mule.tests:mule-tests-model",
        "org.mule.tests:mule-derby-all",
        "org.apache.groovy:groovy",
        "org.apache.groovy:groovy-jsr223"
    },
    extraPrivilegedArtifacts = {"org.mule.tests:mule-tests-parsers-plugin"},
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public interface IntegrationTestCaseRunnerConfig {

}
