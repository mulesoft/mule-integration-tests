/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
