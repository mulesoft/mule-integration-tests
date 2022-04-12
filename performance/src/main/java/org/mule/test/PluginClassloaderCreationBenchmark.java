/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
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
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

@State(Scope.Benchmark)
public class PluginClassloaderCreationBenchmark extends AbstractMuleTestCase {

  public static final String MULE_DOMAIN_FOLDER = "domains";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder artifactLocation = new TemporaryFolder();

  @Rule
  public final SystemProperty muleHomeSystemProperty =
      new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());

  private static final String PRIVILEGED_PACKAGE = "org.foo.privileged";
  private static final String GROUP_ID = "org.mule.test";
  private static final String PLUGIN_ID1 = "plugin1";
  private static final String PLUGIN_ARTIFACT_ID1 = GROUP_ID + ":" + PLUGIN_ID1;
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
  private DomainDescriptor defaultDomainDescriptor;
  private final String customDomainName = "custom-domain";
  private final String onlyDomainPackageName = "domain-package";
  private final String repeatedPackageName = "module&domain-package";
  private final String applicationName = "app";
  private DomainDescriptor customDomainDescriptor;
  private Set<String> domainExportedPackage;
  private List<ArtifactPluginDescriptor> artifactPluginDescriptors;
  private ApplicationDescriptor applicationDescriptor;
  private MuleDeployableArtifactClassLoader customDomainClassLoader;

  @Before
  public void setup() {
    muleHomeFolder = temporaryFolder.getRoot();
    artifactClassLoaderResolver = spy(new DefaultArtifactClassLoaderResolver(moduleRepository, nativeLibraryFinderFactory));

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);
    defaultDomainDescriptor = getTestDomainDescriptor(DEFAULT_DOMAIN_NAME);
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));
    MuleModule muleModule = mock(MuleModule.class);
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
    when(moduleRepository.getModules()).thenReturn(singletonList(muleModule));
    domainExportedPackage = Stream.of(onlyDomainPackageName, repeatedPackageName).collect(toSet());
    artifactPluginDescriptors = Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toList());
    applicationDescriptor = new ApplicationDescriptor(applicationName);
    applicationDescriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    customDomainClassLoader = artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

  @After
  public void tearDown() {
    deleteIfNeeded(getDomainsFolder());
    deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
  }

  private void deleteIfNeeded(File file) {
    if (file.exists()) {
      deleteQuietly(file);
    }
  }

  @Test
  public void createDependantPluginClassLoader() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());
    String plugin2ExportedPackage = "plugin2-package";
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(plugin2ExportedPackage))
            .build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     d -> of(plugin2Descriptor));

  }

  @Test
  public void createPluginClassLoaderWithPrivilegedContainerAccess() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());

    MuleModule privilegedModule = mock(MuleModule.class);
    when(privilegedModule.getPrivilegedArtifacts()).thenReturn(singleton(PLUGIN_ARTIFACT_ID1));
    when(privilegedModule.getPrivilegedExportedPackages()).thenReturn(singleton(PRIVILEGED_PACKAGE));
    when(moduleRepository.getModules()).thenReturn(singletonList(privilegedModule));

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     d -> empty());

  }

  @Test
  public void createsPluginClassLoaderWithPrivilegedPluginAccess() {
    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(singletonList(plugin2Descriptor));

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     d -> of(plugin2Descriptor));

  }

  @Test
  public void createPluginClassLoaderWithExportedLocalPackage() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());
    String pluginPackage = "plugin-package";

    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPackages(singleton(pluginPackage)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().withLocalPackages(singleton(pluginPackage))
            .dependingOn(singleton(pluginDependency)).build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     d -> of(plugin2Descriptor));

  }

  @Test
  public void createPluginClassLoaderWithIgnoredLocalPackages() {
    MuleModule muleModule = mock(MuleModule.class);
    final String package1Name = "module&plugin-package";
    final String package2Name = "org.mule.sdk.api.package";
    when(muleModule.getExportedPackages()).thenReturn(Stream.of(package1Name, package2Name).collect(toSet()));
    when(moduleRepository.getModules()).thenReturn(singletonList(muleModule));

    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());

    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder()
            .withLocalPackages(Stream.of(package1Name, package2Name).collect(toSet())).build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     d -> empty());

  }

  private MuleDeployableArtifactClassLoader getTestDomainClassLoader(List<ArtifactPluginDescriptor> plugins) {

    customDomainDescriptor.setPlugins(new HashSet<>(plugins));
    return artifactClassLoaderResolver.createDomainClassLoader(customDomainDescriptor);
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins) {
    return getTestApplicationClassLoader(plugins, emptySet());
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins,
                                                                          Set<String> exportedPackages) {
    final String applicationName = "app";
    final MuleDeployableArtifactClassLoader domainClassLoader = getTestDomainClassLoader(emptyList());

    ApplicationDescriptor descriptor = new ApplicationDescriptor(applicationName);
    descriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    descriptor.setPlugins(new HashSet<>(plugins));
    descriptor.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(exportedPackages).build());

    return artifactClassLoaderResolver.createApplicationClassLoader(descriptor, () -> domainClassLoader);
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
