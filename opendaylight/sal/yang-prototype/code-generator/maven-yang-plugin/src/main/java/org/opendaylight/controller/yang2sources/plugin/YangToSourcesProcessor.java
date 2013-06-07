/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.parser.impl.YangParserImpl;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.controller.yang2sources.plugin.Util.NamedFileInputStream;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

import com.google.common.collect.Maps;

class YangToSourcesProcessor {
    private static final String LOG_PREFIX = "yang-to-sources:";
    private static final String META_INF_YANG_STRING = "META-INF"
            + File.separator + "yang";
    private static final File META_INF_YANG_DIR = new File(META_INF_YANG_STRING);

    private final Log log;
    private final File yangFilesRootDir;
    private final List<CodeGeneratorArg> codeGenerators;
    private final MavenProject project;
    private final boolean inspectDependencies;

    YangToSourcesProcessor(Log log, File yangFilesRootDir,
            List<CodeGeneratorArg> codeGenerators, MavenProject project,
            boolean inspectDependencies) {
        this.log = checkNotNull(log, "log");
        this.yangFilesRootDir = checkNotNull(yangFilesRootDir,
                "yangFilesRootDir");
        this.codeGenerators = Collections.unmodifiableList(checkNotNull(
                codeGenerators, "codeGenerators"));
        this.project = checkNotNull(project, "project");
        this.inspectDependencies = inspectDependencies;
    }

    private static <T> T checkNotNull(T obj, String paramName) {
        if (obj == null)
            throw new NullPointerException("Parameter '" + paramName
                    + "' is null");
        return obj;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        ContextHolder context = processYang();
        generateSources(context);
        addYangsToMETA_INF();
    }

    private ContextHolder processYang() throws MojoExecutionException {
        YangParserImpl parser = new YangParserImpl();
        List<Closeable> closeables = new ArrayList<>();
        log.info(Util.message("Inspecting %s", LOG_PREFIX, yangFilesRootDir));
        try {
            List<InputStream> yangsInProject = Util
                    .listFilesAsStream(yangFilesRootDir);
            List<InputStream> all = new ArrayList<>(yangsInProject);
            closeables.addAll(yangsInProject);
            Map<InputStream, Module> allYangModules;
            Set<Module> projectYangModules;
            try {
                if (inspectDependencies) {
                    YangsInZipsResult dependentYangResult = findYangFilesInDependenciesAsStream();
                    Closeable dependentYangResult1 = dependentYangResult;
                    closeables.add(dependentYangResult1);
                    all.addAll(dependentYangResult.yangStreams);
                }

                allYangModules = parser.parseYangModelsFromStreamsMapped(all);

                projectYangModules = new HashSet<>();
                for (InputStream inProject : yangsInProject) {
                    projectYangModules.add(allYangModules.get(inProject));
                }

            } finally {
                for (AutoCloseable closeable : closeables) {
                    closeable.close();
                }
            }

            Set<Module> parsedAllYangModules = new HashSet<>(
                    allYangModules.values());
            SchemaContext resolveSchemaContext = parser
                    .resolveSchemaContext(parsedAllYangModules);
            log.info(Util.message("%s files parsed from %s", LOG_PREFIX,
                    Util.YANG_SUFFIX.toUpperCase(), yangsInProject));
            return new ContextHolder(resolveSchemaContext, projectYangModules);

            // MojoExecutionException is thrown since execution cannot continue
        } catch (Exception e) {
            String message = Util.message("Unable to parse %s files from %s",
                    LOG_PREFIX, Util.YANG_SUFFIX, yangFilesRootDir);
            log.error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    private void addYangsToMETA_INF() throws MojoFailureException {
        File targetYangDir = new File(project.getBasedir(), "target"
                + File.separator + "yang");

        try {
            FileUtils.copyDirectory(yangFilesRootDir, targetYangDir);
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Unable to copy yang files into resource folder", e);
        }

        setResource(targetYangDir, META_INF_YANG_DIR.getPath());

        log.debug(Util.message("Yang files from: %s marked as resources: %s",
                LOG_PREFIX, yangFilesRootDir, META_INF_YANG_DIR.getPath()));
    }

    private void setResource(File targetYangDir, String targetPath) {
        Resource res = new Resource();
        res.setDirectory(targetYangDir.getPath());
        if (targetPath != null)
            res.setTargetPath(targetPath);
        project.addResource(res);
    }

    /**
     * Call generate on every generator from plugin configuration
     */
    private void generateSources(ContextHolder context)
            throws MojoFailureException {
        if (codeGenerators.size() == 0) {
            log.warn(Util.message("No code generators provided", LOG_PREFIX));
            return;
        }

        Map<String, String> thrown = Maps.newHashMap();
        for (CodeGeneratorArg codeGenerator : codeGenerators) {
            try {
                generateSourcesWithOneGenerator(context, codeGenerator);
            } catch (Exception e) {
                // try other generators, exception will be thrown after
                log.error(Util.message(
                        "Unable to generate sources with %s generator",
                        LOG_PREFIX, codeGenerator.getCodeGeneratorClass()), e);
                thrown.put(codeGenerator.getCodeGeneratorClass(), e.getClass()
                        .getCanonicalName());
            }
        }

        if (!thrown.isEmpty()) {
            String message = Util
                    .message(
                            "One or more code generators failed, including failed list(generatorClass=exception) %s",
                            LOG_PREFIX, thrown.toString());
            log.error(message);
            throw new MojoFailureException(message);
        }
    }

    /**
     * Instantiate generator from class and call required method
     */
    private void generateSourcesWithOneGenerator(ContextHolder context,
            CodeGeneratorArg codeGeneratorCfg) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IOException {

        codeGeneratorCfg.check();

        CodeGenerator g = Util.getInstance(
                codeGeneratorCfg.getCodeGeneratorClass(), CodeGenerator.class);
        log.info(Util.message("Code generator instantiated from %s",
                LOG_PREFIX, codeGeneratorCfg.getCodeGeneratorClass()));

        File outputDir = codeGeneratorCfg.getOutputBaseDir(project);

        log.info(Util.message("Sources will be generated to %s", LOG_PREFIX,
                outputDir));
        log.debug(Util.message("Project root dir is %s", LOG_PREFIX,
                project.getBasedir()));
        log.debug(Util.message(
                "Additional configuration picked up for : %s: %s", LOG_PREFIX,
                codeGeneratorCfg.getCodeGeneratorClass(),
                codeGeneratorCfg.getAdditionalConfiguration()));

        project.addCompileSourceRoot(outputDir.getAbsolutePath());
        g.setLog(log);
        g.setAdditionalConfig(codeGeneratorCfg.getAdditionalConfiguration());
        File resourceBaseDir = codeGeneratorCfg.getResourceBaseDir(project);

        setResource(resourceBaseDir, null);
        g.setResourceBaseDir(resourceBaseDir);
        log.debug(Util.message(
                "Folder: %s marked as resources for generator: %s", LOG_PREFIX,
                resourceBaseDir, codeGeneratorCfg.getCodeGeneratorClass()));

        Collection<File> generated = g.generateSources(context.getContext(),
                outputDir, context.getYangModules());

        log.info(Util.message("Sources generated by %s: %s", LOG_PREFIX,
                codeGeneratorCfg.getCodeGeneratorClass(), generated));
    }

    private class YangsInZipsResult implements Closeable {
        private final List<InputStream> yangStreams;
        private final List<Closeable> zipInputStreams;

        private YangsInZipsResult(List<InputStream> yangStreams,
                List<Closeable> zipInputStreams) {
            this.yangStreams = yangStreams;
            this.zipInputStreams = zipInputStreams;
        }

        @Override
        public void close() throws IOException {
            for (InputStream is : yangStreams) {
                is.close();
            }
            for (Closeable is : zipInputStreams) {
                is.close();
            }
        }
    }

    private YangsInZipsResult findYangFilesInDependenciesAsStream()
            throws MojoFailureException {
        List<InputStream> yangsFromDependencies = new ArrayList<>();
        List<Closeable> zips = new ArrayList<>();
        try {
            List<File> filesOnCp = Util.getClassPath(project);
            log.info(Util.message(
                    "Searching for yang files in following dependencies: %s",
                    LOG_PREFIX, filesOnCp));

            for (File file : filesOnCp) {
                List<String> foundFilesForReporting = new ArrayList<>();
                // is it jar file or directory?
                if (file.isDirectory()) {
                    File yangDir = new File(file, META_INF_YANG_STRING);
                    if (yangDir.exists() && yangDir.isDirectory()) {
                        File[] yangFiles = yangDir
                                .listFiles(new FilenameFilter() {
                                    @Override
                                    public boolean accept(File dir, String name) {
                                        return name.endsWith(".yang")
                                                && new File(dir, name).isFile();
                                    }
                                });
                        for (File yangFile : yangFiles) {
                            yangsFromDependencies.add(new NamedFileInputStream(
                                    yangFile));
                        }
                    }

                } else {
                    ZipFile zip = new ZipFile(file);
                    zips.add(zip);

                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName.startsWith(META_INF_YANG_STRING)) {
                            if (entry.isDirectory() == false
                                    && entryName.endsWith(".yang")) {
                                foundFilesForReporting.add(entryName);
                                // This will be closed after all strams are
                                // parsed.
                                InputStream entryStream = zip
                                        .getInputStream(entry);
                                yangsFromDependencies.add(entryStream);
                            }
                        }
                    }
                }
                if (foundFilesForReporting.size() > 0) {
                    log.info(Util.message("Found %d yang files in %s: %s",
                            LOG_PREFIX, foundFilesForReporting.size(), file,
                            foundFilesForReporting));
                }

            }
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return new YangsInZipsResult(yangsFromDependencies, zips);
    }

    private class ContextHolder {
        private final SchemaContext context;
        private final Set<Module> yangModules;

        private ContextHolder(SchemaContext context, Set<Module> yangModules) {
            this.context = context;
            this.yangModules = yangModules;
        }

        public SchemaContext getContext() {
            return context;
        }

        public Set<Module> getYangModules() {
            return yangModules;
        }
    }
}
