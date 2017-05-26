/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang2sources.spi.BasicCodeGenerator;
import org.opendaylight.yangtools.yang2sources.spi.MavenProjectAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class interfaces with yang-maven-plugin. Gets parsed yang modules in
 * {@link SchemaContext}, and parameters form the plugin configuration, and
 * writes service interfaces and/or modules.
 */
public class JMXGenerator implements BasicCodeGenerator, MavenProjectAware {
    private static final class NamespaceMapping {
        private final String namespace, packageName;

        public NamespaceMapping(final String namespace, final String packagename) {
            this.namespace = namespace;
            this.packageName = packagename;
        }
    }

    @VisibleForTesting
    static final String NAMESPACE_TO_PACKAGE_DIVIDER = "==";
    @VisibleForTesting
    static final String NAMESPACE_TO_PACKAGE_PREFIX = "namespaceToPackage";
    @VisibleForTesting
    static final String MODULE_FACTORY_FILE_BOOLEAN = "moduleFactoryFile";

    private static final Logger LOG = LoggerFactory.getLogger(JMXGenerator.class);
    private static final Pattern NAMESPACE_MAPPING_PATTERN = Pattern.compile("(.+)" + NAMESPACE_TO_PACKAGE_DIVIDER + "(.+)");

    private final CodeWriter codeWriter;
    private Map<String, String> namespaceToPackageMapping;
    private File resourceBaseDir;
    private File projectBaseDir;
    private boolean generateModuleFactoryFile = true;

    public JMXGenerator() {
        this(new CodeWriter());
    }

    public JMXGenerator(final CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
    }

    @Override
    public Collection<File> generateSources(final SchemaContext context,
                                            final File outputBaseDir, final Set<Module> yangModulesInCurrentMavenModule) {

        Preconditions.checkArgument(context != null, "Null context received");
        Preconditions.checkArgument(outputBaseDir != null,
                "Null outputBaseDir received");

        Preconditions
                .checkArgument((this.namespaceToPackageMapping != null) && !this.namespaceToPackageMapping.isEmpty(),
                        "No namespace to package mapping provided in additionalConfiguration");

        final PackageTranslator packageTranslator = new PackageTranslator(this.namespaceToPackageMapping);

        if (!outputBaseDir.exists()) {
            outputBaseDir.mkdirs();
        }

        final GeneratedFilesTracker generatedFiles = new GeneratedFilesTracker();
        // create SIE structure qNamesToSIEs
        final Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();


        final Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
        for (final Module module : context.getModules()) {
            final String packageName = packageTranslator.getPackageName(module);
            final Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName, knownSEITracker);

            for (final Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries
                    .entrySet()) {
                // merge value into qNamesToSIEs
                if (qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue()) != null) {
                    throw new IllegalStateException(
                        "Cannot add two SIE with same qname "
                                + sieEntry.getValue());
                }
            }
            if (yangModulesInCurrentMavenModule.contains(module)) {
                // write this sie to disk
                for (final ServiceInterfaceEntry sie : namesToSIEntries.values()) {
                    try {
                        generatedFiles.addFile(this.codeWriter.writeSie(sie,
                                outputBaseDir));
                    } catch (final Exception e) {
                        throw new RuntimeException(
                                "Error occurred during SIE source generate phase",
                                e);
                    }
                }
            }
        }

        final File mainBaseDir = concatFolders(this.projectBaseDir, "src", "main", "java");
        Preconditions.checkNotNull(this.resourceBaseDir,
                "resource base dir attribute was null");

        final StringBuilder fullyQualifiedNamesOfFactories = new StringBuilder();
        // create MBEs
        for (final Module module : yangModulesInCurrentMavenModule) {
            final String packageName = packageTranslator.getPackageName(module);
            final Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                    .create(module, qNamesToSIEs, context, new TypeProviderWrapper(new TypeProviderImpl(context)),
                            packageName);

            for (final Entry<String, ModuleMXBeanEntry> mbeEntry : namesToMBEs
                    .entrySet()) {
                final ModuleMXBeanEntry mbe = mbeEntry.getValue();
                try {
                    final List<File> files1 = this.codeWriter.writeMbe(mbe, outputBaseDir,
                            mainBaseDir);
                    generatedFiles.addFile(files1);
                } catch (final Exception e) {
                    throw new RuntimeException(
                            "Error occurred during MBE source generate phase",
                            e);
                }
                fullyQualifiedNamesOfFactories.append(mbe
                        .getFullyQualifiedName(mbe.getStubFactoryName()));
                fullyQualifiedNamesOfFactories.append("\n");
            }
        }
        // create ModuleFactory file if needed
        if ((fullyQualifiedNamesOfFactories.length() > 0)
                && this.generateModuleFactoryFile) {
            final File serviceLoaderFile = JMXGenerator.concatFolders(
                    this.resourceBaseDir, "META-INF", "services",
                    ModuleFactory.class.getName());
            // if this file does not exist, create empty file
            serviceLoaderFile.getParentFile().mkdirs();
            try {
                serviceLoaderFile.createNewFile();
                Files.write(fullyQualifiedNamesOfFactories.toString(), serviceLoaderFile, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                final String message = "Cannot write to " + serviceLoaderFile;
                LOG.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
        return generatedFiles.getFiles();
    }

    @VisibleForTesting
    static File concatFolders(final File projectBaseDir, final String... folderNames) {
        File result = projectBaseDir;
        for (final String folder: folderNames) {
            result = new File(result, folder);
        }
        return result;
    }

    @Override
    public void setAdditionalConfig(final Map<String, String> additionalCfg) {
        LOG.debug("{}: Additional configuration received: {}", getClass().getCanonicalName(), additionalCfg);
        this.namespaceToPackageMapping = extractNamespaceMapping(additionalCfg);
        this.generateModuleFactoryFile = extractModuleFactoryBoolean(additionalCfg);
    }

    private static boolean extractModuleFactoryBoolean(final Map<String, String> additionalCfg) {
        final String bool = additionalCfg.get(MODULE_FACTORY_FILE_BOOLEAN);
        return !"false".equals(bool);
    }

    private static Map<String, String> extractNamespaceMapping(
            final Map<String, String> additionalCfg) {
        final Map<String, String> namespaceToPackage = Maps.newHashMap();
        for (final String key : additionalCfg.keySet()) {
            if (key.startsWith(NAMESPACE_TO_PACKAGE_PREFIX)) {
                final String mapping = additionalCfg.get(key);
                final NamespaceMapping mappingResolved = extractNamespaceMapping(mapping);
                namespaceToPackage.put(mappingResolved.namespace,
                        mappingResolved.packageName);
            }
        }
        return namespaceToPackage;
    }

    private static NamespaceMapping extractNamespaceMapping(final String mapping) {
        final Matcher matcher = NAMESPACE_MAPPING_PATTERN.matcher(mapping);
        Preconditions.checkArgument(matcher.matches(),
            "Namespace to package mapping:%s is in invalid format, requested format is: %s",
            mapping, NAMESPACE_MAPPING_PATTERN);
        return new NamespaceMapping(matcher.group(1), matcher.group(2));
    }

    @Override
    public void setResourceBaseDir(final File resourceDir) {
        this.resourceBaseDir = resourceDir;
    }

    @Override
    public void setMavenProject(final MavenProject project) {
        this.projectBaseDir = project.getBasedir();
        LOG.debug("{}: project base dir: {}", getClass().getCanonicalName(), this.projectBaseDir);
    }

    @VisibleForTesting
    static class GeneratedFilesTracker {
        private final Set<File> files = Sets.newHashSet();

        void addFile(final File file) {
            if (this.files.contains(file)) {
                final List<File> undeletedFiles = Lists.newArrayList();
                for (final File presentFile : this.files) {
                    if (!presentFile.delete()) {
                        undeletedFiles.add(presentFile);
                    }
                }
                if (!undeletedFiles.isEmpty()) {
                    LOG.error(
                            "Illegal state occurred: Unable to delete already generated files, undeleted files: {}",
                            undeletedFiles);
                }
                throw new IllegalStateException(
                        "Name conflict in generated files, file" + file
                                + " present twice");
            }
            this.files.add(file);
        }

        void addFile(final Collection<File> files) {
            for (final File file : files) {
                addFile(file);
            }
        }

        public Set<File> getFiles() {
            return this.files;
        }
    }
}
