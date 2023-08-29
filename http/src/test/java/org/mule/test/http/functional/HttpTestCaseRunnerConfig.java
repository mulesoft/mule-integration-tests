/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public interface HttpTestCaseRunnerConfig {

}
