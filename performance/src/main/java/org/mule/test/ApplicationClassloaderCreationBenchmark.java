/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.junit.rules.TemporaryFolder;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Scope.Benchmark)
public class ApplicationClassloaderCreationBenchmark extends AbstractMuleTestCase {

  public static final String MULE_DOMAIN_FOLDER = "domains";

  private static final String GROUP_ID = "org.mule.test";
  private static final String PLUGIN_ID1 = "plugin1";
  private static final String PLUGIN_ID2 = "plugin2";
  private static final BundleDescriptor PLUGIN1_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID1)
          .setVersion("1.0").setClassifier("mule-plugin").build();
  private static final BundleDescriptor PLUGIN2_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID2)
          .setVersion("1.0").setClassifier("mule-plugin").build();

  private final ArtifactPluginDescriptor plugin1Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID1);
  private final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID2);
  private File muleHomeFolder;

  private DefaultArtifactClassLoaderResolver artifactClassLoaderResolver;
  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();
  private final String customDomainName = "custom-domain";
  private final String repeatedPackageName = "module&app-package";
  private final String onlyAppPackageName = "app-package";
  private final String applicationName = "app";
  private DomainDescriptor customDomainDescriptor;
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors;
  private ApplicationDescriptor applicationDescriptor;
  private MuleDeployableArtifactClassLoader customDomainClassLoader;
  private List<MuleModule> muleModuleSingletonList;
  private TemporaryFolder artifactLocation;
  private MuleDeployableArtifactClassLoader domainClassLoader;
  private ClassLoaderModel classLoaderModel;

  @Setup
  public void setup() throws IOException {
    TemporaryFolder temporaryFolder = new TemporaryFolder();
    artifactLocation = new TemporaryFolder();
    temporaryFolder.create();
    artifactLocation.create();
    muleHomeFolder = temporaryFolder.getRoot();
    System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());
    artifactClassLoaderResolver = spy(new DefaultArtifactClassLoaderResolver(moduleRepository, nativeLibraryFinderFactory));

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));

    MuleModule muleModule = mock(MuleModule.class);
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
    muleModuleSingletonList = singletonList(muleModule);

    artifactPluginDescriptors = Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet());
    applicationDescriptor = new ApplicationDescriptor(applicationName);
    applicationDescriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    customDomainClassLoader = artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
    domainClassLoader = getTestDomainClassLoader(emptyList());
    Set<String> applicationExportedPackage = Stream.of(onlyAppPackageName, repeatedPackageName).collect(toSet());
    classLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(applicationExportedPackage).build();
  }

  @TearDown
  public void tearDown() {
    deleteIfNeeded(getDomainsFolder());
    deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
    System.clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
  }

  private void deleteIfNeeded(File file) {
    if (file.exists()) {
      deleteQuietly(file);
    }
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoader() {
    return artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> customDomainClassLoader);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoaderWithExportedPackages() {
    when(moduleRepository.getModules()).thenReturn(muleModuleSingletonList);
    applicationDescriptor.setClassLoaderModel(classLoaderModel);
    return artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> domainClassLoader);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoaderWithPlugins() {
    applicationDescriptor.setPlugins(artifactPluginDescriptors);
    return artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> domainClassLoader);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoaderWithCachedPlugin() {
    MuleDeployableArtifactClassLoader applicationClassLoader =
        artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> domainClassLoader);

    final ApplicationDescriptor newApplicationDescriptor = applicationClassLoader.getArtifactDescriptor();
    newApplicationDescriptor.setPlugins(artifactPluginDescriptors);

    final MuleArtifactClassLoader domainClassLoader = (MuleArtifactClassLoader) applicationClassLoader.getParent().getParent();

    final MuleArtifactClassLoader plugin2ClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin2Descriptor,
                                     d -> empty());

    return artifactClassLoaderResolver.createApplicationClassLoader(newApplicationDescriptor,
                                                                    () -> domainClassLoader,
                                                                    (ownerClassLoader, pluginDescriptor) -> {
                                                                      if (pluginDescriptor
                                                                          .getBundleDescriptor()
                                                                          .getArtifactId()
                                                                          .equals(plugin2Descriptor
                                                                              .getBundleDescriptor()
                                                                              .getArtifactId())) {
                                                                        return of(() -> plugin2ClassLoader);
                                                                      } else {
                                                                        return empty();
                                                                      }
                                                                    });

  }

  private MuleDeployableArtifactClassLoader getTestDomainClassLoader(List<ArtifactPluginDescriptor> plugins) {
    customDomainDescriptor.setPlugins(new HashSet<>(plugins));
    return artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

  private DomainDescriptor getTestDomainDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor(name);
    descriptor.setRedeploymentEnabled(false);
    descriptor.setArtifactLocation(artifactLocation.getRoot());
    return descriptor;
  }

  protected File createDomainDir(String domainFolder, String domain) {
    final File file = new File(muleHomeFolder, domainFolder + File.separator + domain);
    assertThat(file.mkdirs(), is(true));
    return file;
  }
}
