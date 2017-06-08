/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit", "org.mule.tests:mule-tests-functional",
    "org.mule.tests:mule-tests-component-plugin"})
public interface IntegrationTestCaseRunnerConfig {

}
