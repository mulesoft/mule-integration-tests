/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mockito.Mockito.when;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class DomainClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

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
    super.setup();
    defaultDomainDescriptor = getTestDomainDescriptor(DEFAULT_DOMAIN_NAME);
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
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
    customDomainDescriptor.setClassLoaderModel(classLoaderModel);
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
