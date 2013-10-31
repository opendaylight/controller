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
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang2sources.spi.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class interfaces with yang-maven-plugin. Gets parsed yang modules in
 * {@link SchemaContext}, and parameters form the plugin configuration, and
 * writes service interfaces and/or modules.
 */
public class JMXGenerator implements CodeGenerator {

    static final String NAMESPACE_TO_PACKAGE_DIVIDER = "==";
    static final String NAMESPACE_TO_PACKAGE_PREFIX = "namespaceToPackage";
    static final String MODULE_FACTORY_FILE_BOOLEAN = "moduleFactoryFile";

    private PackageTranslator packageTranslator;
    private final CodeWriter codeWriter;
    private static final Logger logger = LoggerFactory
            .getLogger(JMXGenerator.class);
    private Map<String, String> namespaceToPackageMapping;
    private File resourceBaseDir;
    private File projectBaseDir;
    private boolean generateModuleFactoryFile = true;

    public JMXGenerator() {
        this.codeWriter = new FreeMarkerCodeWriterImpl();
    }

    public JMXGenerator(CodeWriter codeWriter) {
        this.codeWriter = codeWriter;
    }

    @Override
    public Collection<File> generateSources(SchemaContext context,
            File outputBaseDir, Set<Module> yangModulesInCurrentMavenModule) {

        Preconditions.checkArgument(context != null, "Null context received");
        Preconditions.checkArgument(outputBaseDir != null,
                "Null outputBaseDir received");

        Preconditions
                .checkArgument(namespaceToPackageMapping != null && !namespaceToPackageMapping.isEmpty(),
                        "No namespace to package mapping provided in additionalConfiguration");

        packageTranslator = new PackageTranslator(namespaceToPackageMapping);

        if (!outputBaseDir.exists())
            outputBaseDir.mkdirs();

        GeneratedFilesTracker generatedFiles = new GeneratedFilesTracker();
        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();

        // create SIE structure qNamesToSIEs
        for (Module module : context.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName);

            for (Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries
                    .entrySet()) {

                // merge value into qNamesToSIEs
                if (qNamesToSIEs.containsKey(sieEntry.getKey()) == false) {
                    qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue());
                } else {
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

        StringBuffer fullyQualifiedNamesOfFactories = new StringBuffer();
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
                            mainBaseDir, resourceBaseDir);
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
                FileUtils.write(serviceLoaderFile,
                        fullyQualifiedNamesOfFactories.toString());
            } catch (IOException e) {
                String message = "Cannot write to " + serviceLoaderFile;
                logger.error(message);
                throw new RuntimeException(message, e);
            }
        }
        return generatedFiles.getFiles();
    }

    static File concatFolders(File projectBaseDir, String... folderNames) {
        StringBuilder b = new StringBuilder();
        for (String folder : folderNames) {
            b.append(folder);
            b.append(File.separator);
        }
        return new File(projectBaseDir, b.toString());
    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalCfg) {
        if (logger != null)
            logger.debug(getClass().getCanonicalName(),
                    ": Additional configuration received: ",
                    additionalCfg.toString());
        this.namespaceToPackageMapping = extractNamespaceMapping(additionalCfg);
        this.generateModuleFactoryFile = extractModuleFactoryBoolean(additionalCfg);
    }

    private boolean extractModuleFactoryBoolean(
            Map<String, String> additionalCfg) {
        String bool = additionalCfg.get(MODULE_FACTORY_FILE_BOOLEAN);
        if (bool == null)
            return true;
        if (bool.equals("false"))
            return false;
        return true;
    }

    @Override
    public void setLog(Log log) {
        StaticLoggerBinder.getSingleton().setLog(log);
    }

    private static Map<String, String> extractNamespaceMapping(
            Map<String, String> additionalCfg) {
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

    static Pattern namespaceMappingPattern = Pattern.compile("(.+)"
            + NAMESPACE_TO_PACKAGE_DIVIDER + "(.+)");

    private static NamespaceMapping extractNamespaceMapping(String mapping) {
        Matcher matcher = namespaceMappingPattern.matcher(mapping);
        Preconditions
                .checkArgument(matcher.matches(), String.format("Namespace to package mapping:%s is in invalid " +
                        "format, requested format is: %s", mapping, namespaceMappingPattern));
        return new NamespaceMapping(matcher.group(1), matcher.group(2));
    }

    private static class NamespaceMapping {
        public NamespaceMapping(String namespace, String packagename) {
            this.namespace = namespace;
            this.packageName = packagename;
        }

        private final String namespace, packageName;
    }

    @Override
    public void setResourceBaseDir(File resourceDir) {
        this.resourceBaseDir = resourceDir;
    }

    @Override
    public void setMavenProject(MavenProject project) {
        this.projectBaseDir = project.getBasedir();

        if (logger != null)
            logger.debug(getClass().getCanonicalName(), " project base dir: ",
                    projectBaseDir);
    }

    @VisibleForTesting
    static class GeneratedFilesTracker {
        private final Set<File> files = Sets.newHashSet();

        void addFile(File file) {
            if (files.contains(file)) {
                List<File> undeletedFiles = Lists.newArrayList();
                for (File presentFile : files) {
                    if (presentFile.delete() == false) {
                        undeletedFiles.add(presentFile);
                    }
                }
                if (undeletedFiles.isEmpty() == false) {
                    logger.error(
                            "Illegal state occurred: Unable to delete already generated files, undeleted files: {}",
                            undeletedFiles);
                }
                throw new IllegalStateException(
                        "Name conflict in generated files, file" + file
                                + " present twice");
            }
            files.add(file);
        }

        void addFile(Collection<File> files) {
            for (File file : files) {
                addFile(file);
            }
        }

        public Set<File> getFiles() {
            return files;
        }
    }
}
