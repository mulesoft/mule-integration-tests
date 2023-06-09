/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
        "org.mule.tests:mule-tests-model",
        "org.apache.derby:derby",
        "org.codehaus.groovy:groovy-all"
    },
    extraPrivilegedArtifacts = {"org.mule.tests:mule-tests-parsers-plugin"},
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public interface IntegrationTestCaseRunnerConfig {

}
