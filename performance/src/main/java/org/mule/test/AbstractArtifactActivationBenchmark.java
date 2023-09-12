/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import static java.io.File.separator;
import static java.util.Collections.emptyList;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.openjdk.jmh.annotations.TearDown;

import org.junit.rules.TemporaryFolder;

public abstract class AbstractArtifactActivationBenchmark extends AbstractMuleTestCase {

  protected static final String MULE_DOMAIN_FOLDER = "domains";
  protected static final String GROUP_ID = "org.mule.test";
  protected static final String PLUGIN_ID1 = "plugin1";
  protected static final String PLUGIN_ID2 = "plugin2";
  protected static final BundleDescriptor PLUGIN1_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID1)
          .setVersion("1.0").setClassifier("mule-plugin").build();
  protected static final BundleDescriptor PLUGIN2_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID2)
          .setVersion("1.0").setClassifier("mule-plugin").build();

  protected DomainDescriptor customDomainDescriptor;
  protected File muleHomeFolder;
  protected TemporaryFolder artifactLocation;
  protected DefaultArtifactClassLoaderResolver artifactClassLoaderResolver;

  protected final ModuleRepository moduleRepository = new DummyModuleRepository(emptyList());
  protected final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();
  protected final ArtifactPluginDescriptor plugin1Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID1);
  protected final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID2);
  protected DefaultArtifactClassLoaderResolver artifactClassLoaderResolverWithModules;

  public void setup() throws IOException {
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    artifactLocation = new TemporaryFolder();
    temporaryFolder.create();
    artifactLocation.create();
    muleHomeFolder = temporaryFolder.getRoot();
    System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());
    artifactClassLoaderResolver = new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepository),
                                                                         moduleRepository, nativeLibraryFinderFactory);

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);
  }

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

  public static class DummyModuleRepository implements ModuleRepository {

    private final List<MuleContainerModule> muleModules;

    public DummyModuleRepository(List<MuleContainerModule> muleModules) {
      this.muleModules = muleModules;
    }

    @Override
    public List<MuleContainerModule> getModules() {
      return muleModules;
    }
  }

}
