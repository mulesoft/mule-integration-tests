/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    applicationSharedRuntimeLibs = {
        "org.springframework:spring-core",
        "org.springframework:spring-beans",
        "org.springframework:spring-context",
        "org.springframework:spring-aop",
        "org.springframework:spring-expression",
        "org.springframework.security:spring-security-core",
        "org.springframework.security:spring-security-config"
    },
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public interface IntegrationTestCaseRunnerConfig {

}
