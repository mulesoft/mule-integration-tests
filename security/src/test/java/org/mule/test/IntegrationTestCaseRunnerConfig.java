/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
