/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.junit.rules.TemporaryFolder;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Scope.Benchmark)
public class DomainClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

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

  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();
  private DomainDescriptor defaultDomainDescriptor;
  private final String customDomainName = "custom-domain";
  private final String onlyDomainPackageName = "domain-package";
  private final String repeatedPackageName = "module&domain-package";
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors;
  private ClassLoaderModel classLoaderModel;
  private List<MuleModule> muleModuleSingletonList;
  private MuleDeployableArtifactClassLoader domainClassLoaderForCache;
  private DomainDescriptor newDomainDescriptorForCache;
  private MuleArtifactClassLoader plugin2ClassLoaderForCache;

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
    defaultDomainDescriptor = getTestDomainDescriptor(DEFAULT_DOMAIN_NAME);
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));
    MuleModule muleModule = mock(MuleModule.class);
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
    muleModuleSingletonList = singletonList(muleModule);
    Set<String> domainExportedPackage = Stream.of(onlyDomainPackageName, repeatedPackageName).collect(toSet());
    artifactPluginDescriptors = Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet());
    classLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(domainExportedPackage).build();

    domainClassLoaderForCache = artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
    newDomainDescriptorForCache = domainClassLoaderForCache.getArtifactDescriptor();
    newDomainDescriptorForCache.setPlugins(artifactPluginDescriptors);
    plugin2ClassLoaderForCache =
        artifactClassLoaderResolver.createMulePluginClassLoader(domainClassLoaderForCache, plugin2Descriptor, d -> empty());
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainDefaultClassLoader() {
    return artifactClassLoaderResolver.createDomainClassLoader(defaultDomainDescriptor);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainClassLoaderWithExportedPackages() {
    when(moduleRepository.getModules()).thenReturn(muleModuleSingletonList);
    customDomainDescriptor.setClassLoaderModel(classLoaderModel);
    return artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainClassLoaderWithPlugins() {
    customDomainDescriptor.setPlugins(artifactPluginDescriptors);
    return artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainClassLoaderWithCachedPlugin() {
    return artifactClassLoaderResolver.createDomainClassLoader(newDomainDescriptorForCache,
                                                               (ownerClassLoader, pluginDescriptor) -> {
                                                                 if (pluginDescriptor
                                                                     .getBundleDescriptor()
                                                                     .getArtifactId()
                                                                     .equals(plugin2Descriptor
                                                                         .getBundleDescriptor()
                                                                         .getArtifactId())) {
                                                                   return of(() -> plugin2ClassLoaderForCache);
                                                                 } else {
                                                                   return empty();
                                                                 }
                                                               });

  }
}
