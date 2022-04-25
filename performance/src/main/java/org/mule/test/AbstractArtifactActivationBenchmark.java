/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.io.File.separator;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.junit.rules.TemporaryFolder;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.openjdk.jmh.annotations.TearDown;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractArtifactActivationBenchmark extends AbstractMuleTestCase {

  protected DomainDescriptor customDomainDescriptor;
  protected File muleHomeFolder;
  protected TemporaryFolder artifactLocation;
  protected DefaultArtifactClassLoaderResolver artifactClassLoaderResolver;

  @TearDown
  public void tearDown() {
    deleteIfNeeded(getDomainsFolder());
    deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
    System.clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
  }

  protected void deleteIfNeeded(File file) {
    if (file.exists()) {
      deleteQuietly(file);
    }
  }

  protected DomainDescriptor getTestDomainDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor(name);
    descriptor.setRedeploymentEnabled(false);
    descriptor.setArtifactLocation(artifactLocation.getRoot());
    return descriptor;
  }

  protected File createDomainDir(String domainFolder, String domain) {
    final File file = new File(muleHomeFolder, domainFolder + separator + domain);
    assertThat(file.mkdirs(), is(true));
    return file;
  }

  protected MuleDeployableArtifactClassLoader getTestDomainClassLoader(List<ArtifactPluginDescriptor> plugins) {
    customDomainDescriptor.setPlugins(new HashSet<>(plugins));
    return artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

}
