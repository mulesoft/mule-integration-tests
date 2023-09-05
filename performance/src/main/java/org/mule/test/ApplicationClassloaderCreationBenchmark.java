/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;

import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class ApplicationClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

  private final String customDomainName = "custom-domain";
  private final String repeatedPackageName = "module&app-package";
  private final String onlyAppPackageName = "app-package";
  private final String applicationName = "app";
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors;
  private ApplicationDescriptor applicationDescriptor;
  private MuleDeployableArtifactClassLoader customDomainClassLoader;
  private MuleDeployableArtifactClassLoader domainClassLoader;
  private ClassLoaderConfiguration classLoaderConfiguration;
  private MuleDeployableArtifactClassLoader applicationClassLoaderForCache;
  private ApplicationDescriptor newApplicationDescriptorForCache;
  private MuleArtifactClassLoader plugin2ClassLoaderForCache;
  private MuleModule muleModule;
  private List<MuleContainerModule> muleModuleSingletonList;

  @Override
  @Setup
  public void setup() throws IOException {
    super.setup();
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));

    muleModule = new MuleModule("TEST", singleton(repeatedPackageName), emptySet(), emptySet(), emptySet(), emptyList());
    muleModuleSingletonList = singletonList(muleModule);
    ModuleRepository moduleRepositoryWithModules = new DummyModuleRepository(muleModuleSingletonList);
    artifactClassLoaderResolverWithModules =
        new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepositoryWithModules),
                                               moduleRepositoryWithModules, nativeLibraryFinderFactory);

    artifactPluginDescriptors = Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet());
    applicationDescriptor = new ApplicationDescriptor(applicationName);
    applicationDescriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    customDomainClassLoader = artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
    domainClassLoader = getTestDomainClassLoader(emptyList());
    Set<String> applicationExportedPackage = Stream.of(onlyAppPackageName, repeatedPackageName).collect(toSet());
    classLoaderConfiguration = new ClassLoaderConfigurationBuilder().exportingPackages(applicationExportedPackage).build();

    applicationClassLoaderForCache =
        artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> domainClassLoader);
    newApplicationDescriptorForCache = applicationClassLoaderForCache.getArtifactDescriptor();
    newApplicationDescriptorForCache.setPlugins(artifactPluginDescriptors);
    plugin2ClassLoaderForCache = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoaderForCache, plugin2Descriptor, (apds, d) -> empty());
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoader() {
    return artifactClassLoaderResolver.createApplicationClassLoader(applicationDescriptor, () -> customDomainClassLoader);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createApplicationClassLoaderWithExportedPackages() {
    applicationDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
    return artifactClassLoaderResolverWithModules.createApplicationClassLoader(applicationDescriptor,
                                                                               () -> domainClassLoader);
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
    return artifactClassLoaderResolver
        .createApplicationClassLoader(newApplicationDescriptorForCache,
                                      () -> (MuleArtifactClassLoader) applicationClassLoaderForCache.getParent().getParent(),
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
