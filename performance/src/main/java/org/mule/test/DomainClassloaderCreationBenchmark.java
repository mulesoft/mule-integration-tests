/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;

import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

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
public class DomainClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

  private DomainDescriptor defaultDomainDescriptor;
  private final String customDomainName = "custom-domain";
  private final String onlyDomainPackageName = "domain-package";
  private final String repeatedPackageName = "module&domain-package";
  private Set<ArtifactPluginDescriptor> artifactPluginDescriptors;
  private ClassLoaderConfiguration classLoaderConfiguration;
  private MuleDeployableArtifactClassLoader domainClassLoaderForCache;
  private DomainDescriptor newDomainDescriptorForCache;
  private MuleArtifactClassLoader plugin2ClassLoaderForCache;

  private MuleModule muleModule;
  private List<MuleModule> muleModuleSingletonList;

  @Override
  @Setup
  public void setup() throws IOException {
    super.setup();
    defaultDomainDescriptor = getTestDomainDescriptor(DEFAULT_DOMAIN_NAME);
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));
    muleModule = new MuleModule("TEST", singleton(repeatedPackageName), emptySet(), emptySet(), emptySet(), emptyList());
    muleModuleSingletonList = singletonList(muleModule);
    ModuleRepository moduleRepositoryWithModules = new DummyModuleRepository(muleModuleSingletonList);
    artifactClassLoaderResolverWithModules =
        new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepositoryWithModules),
                                               moduleRepositoryWithModules, nativeLibraryFinderFactory);

    Set<String> domainExportedPackage = Stream.of(onlyDomainPackageName, repeatedPackageName).collect(toSet());
    artifactPluginDescriptors = Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet());
    classLoaderConfiguration = new ClassLoaderConfigurationBuilder().exportingPackages(domainExportedPackage).build();

    domainClassLoaderForCache = artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
    newDomainDescriptorForCache = domainClassLoaderForCache.getArtifactDescriptor();
    newDomainDescriptorForCache.setPlugins(artifactPluginDescriptors);
    plugin2ClassLoaderForCache =
        artifactClassLoaderResolver.createMulePluginClassLoader(domainClassLoaderForCache, plugin2Descriptor,
                                                                (apds, d) -> empty());
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainDefaultClassLoader() {
    return artifactClassLoaderResolver.createDomainClassLoader(defaultDomainDescriptor);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleDeployableArtifactClassLoader createDomainClassLoaderWithExportedPackages() {
    customDomainDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
    return artifactClassLoaderResolverWithModules.createDomainClassLoader(customDomainDescriptor);
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
