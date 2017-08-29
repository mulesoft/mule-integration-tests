/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {
    "org.mule.tests:mule-tests-unit",
    "org.springframework:spring-core",
    "org.springframework:spring-beans",
    "org.springframework:spring-context",
    "org.springframework.security:spring-security-core",
    "org.springframework.security:spring-security-config"
},
    extraPrivilegedArtifacts = {"org.mule.tests:mule-tests-parsers-plugin"})
public interface IntegrationTestCaseRunnerConfig {

}
