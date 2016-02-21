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
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
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
                .checkArgument(namespaceToPackageMapping != null && !namespaceToPackageMapping.isEmpty(),
                        "No namespace to package mapping provided in additionalConfiguration");

        PackageTranslator packageTranslator = new PackageTranslator(namespaceToPackageMapping);

        if (!outputBaseDir.exists()) {
            outputBaseDir.mkdirs();
        }

        GeneratedFilesTracker generatedFiles = new GeneratedFilesTracker();
        // create SIE structure qNamesToSIEs
        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();


        Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
        for (Module module : context.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName, knownSEITracker);

            for (Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries
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
                for (ServiceInterfaceEntry sie : namesToSIEntries.values()) {
                    try {
                        generatedFiles.addFile(codeWriter.writeSie(sie,
                                outputBaseDir));
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Error occurred during SIE source generate phase",
                                e);
                    }
                }
            }
        }

        File mainBaseDir = concatFolders(projectBaseDir, "src", "main", "java");
        Preconditions.checkNotNull(resourceBaseDir,
                "resource base dir attribute was null");

        StringBuilder fullyQualifiedNamesOfFactories = new StringBuilder();
        // create MBEs
        for (Module module : yangModulesInCurrentMavenModule) {
            String packageName = packageTranslator.getPackageName(module);
            Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs = ModuleMXBeanEntry
                    .create(module, qNamesToSIEs, context, new TypeProviderWrapper(new TypeProviderImpl(context)),
                            packageName);

            for (Entry<String, ModuleMXBeanEntry> mbeEntry : namesToMBEs
                    .entrySet()) {
                ModuleMXBeanEntry mbe = mbeEntry.getValue();
                try {
                    List<File> files1 = codeWriter.writeMbe(mbe, outputBaseDir,
                            mainBaseDir);
                    generatedFiles.addFile(files1);
                } catch (Exception e) {
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
        if (fullyQualifiedNamesOfFactories.length() > 0
                && generateModuleFactoryFile) {
            File serviceLoaderFile = JMXGenerator.concatFolders(
                    resourceBaseDir, "META-INF", "services",
                    ModuleFactory.class.getName());
            // if this file does not exist, create empty file
            serviceLoaderFile.getParentFile().mkdirs();
            try {
                serviceLoaderFile.createNewFile();
                Files.write(fullyQualifiedNamesOfFactories.toString(), serviceLoaderFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                String message = "Cannot write to " + serviceLoaderFile;
                LOG.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
        return generatedFiles.getFiles();
    }

    @VisibleForTesting
    static File concatFolders(final File projectBaseDir, final String... folderNames) {
        File result = projectBaseDir;
        for (String folder: folderNames) {
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
        String bool = additionalCfg.get(MODULE_FACTORY_FILE_BOOLEAN);
        return !"false".equals(bool);
    }

    private static Map<String, String> extractNamespaceMapping(
            final Map<String, String> additionalCfg) {
        Map<String, String> namespaceToPackage = Maps.newHashMap();
        for (String key : additionalCfg.keySet()) {
            if (key.startsWith(NAMESPACE_TO_PACKAGE_PREFIX)) {
                String mapping = additionalCfg.get(key);
                NamespaceMapping mappingResolved = extractNamespaceMapping(mapping);
                namespaceToPackage.put(mappingResolved.namespace,
                        mappingResolved.packageName);
            }
        }
        return namespaceToPackage;
    }

    private static NamespaceMapping extractNamespaceMapping(final String mapping) {
        Matcher matcher = NAMESPACE_MAPPING_PATTERN.matcher(mapping);
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
        LOG.debug("{}: project base dir: {}", getClass().getCanonicalName(), projectBaseDir);
    }

    @VisibleForTesting
    static class GeneratedFilesTracker {
        private final Set<File> files = Sets.newHashSet();

        void addFile(final File file) {
            if (files.contains(file)) {
                List<File> undeletedFiles = Lists.newArrayList();
                for (File presentFile : files) {
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
            files.add(file);
        }

        void addFile(final Collection<File> files) {
            for (File file : files) {
                addFile(file);
            }
        }

        public Set<File> getFiles() {
            return files;
        }
    }
}
