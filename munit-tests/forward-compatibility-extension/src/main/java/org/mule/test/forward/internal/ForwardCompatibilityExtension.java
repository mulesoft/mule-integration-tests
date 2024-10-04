/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.forward.internal;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;

@Xml(prefix = "forward-compatibility")
@Extension(name = "Forward-Compatibility")
@JavaVersionSupport({JAVA_17, JAVA_21})
@Configurations(ForwardCompatibilityConfiguration.class)
public class ForwardCompatibilityExtension {

}
