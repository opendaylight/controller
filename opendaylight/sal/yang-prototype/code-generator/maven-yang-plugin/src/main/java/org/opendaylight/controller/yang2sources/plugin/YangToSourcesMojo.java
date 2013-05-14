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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.parser.impl.YangParserImpl;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.ResourceProviderArg;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;
import org.opendaylight.controller.yang2sources.spi.ResourceGenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Generate sources from yang files using user provided set of
 * {@link CodeGenerator}s. Steps of this process:
 * <ol>
 * <li>List yang files from {@link #yangFilesRootDir}</li>
 * <li>Process yang files using {@link YangModelParserImpl}</li>
 * <li>For each {@link CodeGenerator} from {@link #codeGenerators}:</li>
 * <ol>
 * <li>Instantiate using default constructor</li>
 * <li>Call {@link CodeGenerator#generateSources(SchemaContext, File)}</li>
 * </ol>
 * </ol>
 */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public final class YangToSourcesMojo extends AbstractMojo {
    private static final String LOG_PREFIX = "yang-to-sources:";
    private static final String INPUT_RESOURCE_DIR = "META-INF/yangs/";
    private static final String OUTPUT_RESOURCE_DIR = "/target/external-resources/";

    /**
     * Classes implementing {@link CodeGenerator} interface. An instance will be
     * created out of every class using default constructor. Method
     * {@link CodeGenerator#generateSources(SchemaContext, File)} will be called
     * on every instance.
     */
    @Parameter(required = true)
    private CodeGeneratorArg[] codeGenerators;

    /**
     * Source directory that will be recursively searched for yang files (ending
     * with .yang suffix).
     */
    @Parameter(required = true)
    private String yangFilesRootDir;

    /**
     * Classes implementing {@link ResourceGenerator} interface. An instance
     * will be created out of every class using default constructor. Method
     * {@link ResourceGenerator#generateResourceFiles(Collection, File)} will be
     * called on every instance.
     */
    @Parameter(required = true)
    private ResourceProviderArg[] resourceProviders;

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    private transient final YangModelParser parser;

    @VisibleForTesting
    YangToSourcesMojo(ResourceProviderArg[] resourceProviderArgs,
            CodeGeneratorArg[] codeGeneratorArgs, YangModelParser parser,
            String yangFilesRootDir) {
        super();
        this.resourceProviders = resourceProviderArgs;
        this.codeGenerators = codeGeneratorArgs;
        this.yangFilesRootDir = yangFilesRootDir;
        this.parser = parser;
    }

    public YangToSourcesMojo() {
        super();
        parser = new YangParserImpl();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        SchemaContext context = processYang();
        generateSources(context);
        generateResources();

        closeResources();
    }

    /**
     * Generate {@link SchemaContext} with {@link YangModelParserImpl}
     */
    private SchemaContext processYang() throws MojoExecutionException {
        try {
            Collection<InputStream> yangFiles = Util
                    .listFilesAsStream(yangFilesRootDir);
            yangFiles.addAll(getFilesFromDependenciesAsStream());

            if (yangFiles.isEmpty()) {
                getLog().warn(
                        Util.message("No %s file found in %s", LOG_PREFIX,
                                Util.YANG_SUFFIX, yangFilesRootDir));
                return null;
            }

            Set<Module> parsedYang = parser
                    .parseYangModelsFromStreams(new ArrayList<InputStream>(
                            yangFiles));
            SchemaContext resolveSchemaContext = parser
                    .resolveSchemaContext(parsedYang);
            getLog().info(
                    Util.message("%s files parsed from %s", LOG_PREFIX,
                            Util.YANG_SUFFIX, yangFiles));
            return resolveSchemaContext;

            // MojoExecutionException is thrown since execution cannot continue
        } catch (Exception e) {
            String message = Util.message("Unable to parse %s files from %s",
                    LOG_PREFIX, Util.YANG_SUFFIX, yangFilesRootDir);
            getLog().error(message, e);
            throw new MojoExecutionException(message, e);
        }
    }

    private void generateResources() throws MojoExecutionException,
            MojoFailureException {
        if (resourceProviders.length == 0) {
            getLog().warn(
                    Util.message("No resource provider classes provided",
                            LOG_PREFIX));
            return;
        }

        Resource res = new Resource();
        String baseDirName = project.getBasedir().getAbsolutePath();
        res.setDirectory(baseDirName + OUTPUT_RESOURCE_DIR);
        res.setTargetPath(INPUT_RESOURCE_DIR);
        project.addResource(res);

        Map<String, String> thrown = Maps.newHashMap();

        Collection<File> yangFiles = new ArrayList<File>();

        // load files from yang root
        yangFiles.addAll(getFilesFromYangRoot());

        // load files from dependencies
        yangFiles.addAll(getFilesFromDependencies());


        for (ResourceProviderArg resourceProvider : resourceProviders) {
            try {
                provideResourcesWithOneProvider(yangFiles, resourceProvider);
            } catch (Exception e) {
                // try other generators, exception will be thrown after
                getLog().error(
                        Util.message(
                                "Unable to provide resources with %s resource provider",
                                LOG_PREFIX,
                                resourceProvider.getResourceProviderClass()), e);
                thrown.put(resourceProvider.getResourceProviderClass(), e
                        .getClass().getCanonicalName());
            }
        }

        if (!thrown.isEmpty()) {
            String message = Util
                    .message(
                            "One or more code resource provider failed, including failed list(resourceProviderClass=exception) %s",
                            LOG_PREFIX, thrown.toString());
            getLog().error(message);
            throw new MojoFailureException(message);
        }
    }

    private Collection<File> getFilesFromYangRoot() {
        Collection<File> yangFilesLoaded = Util.listFiles(yangFilesRootDir);
        Collection<File> yangFiles = new ArrayList<File>(yangFilesLoaded);

        try {
            for(File yangFile : yangFilesLoaded) {
                InputStream is = new FileInputStream(yangFile);
                yangFiles.add(createFileFromStream(is, project.getBasedir().getAbsolutePath() + OUTPUT_RESOURCE_DIR + yangFile.getName()));
                resources.add(is);
            }
        } catch(IOException e) {
            getLog().warn("Exception while loading yang files.", e);
        }
        return yangFiles;
    }

    private Collection<File> getFilesFromDependencies() {
        Collection<File> yangFiles = new ArrayList<File>();

        try {
            List<File> filesOnCp = Util.getClassPath(project);
            List<String> filter = Lists.newArrayList(".yang");
            for (File file : filesOnCp) {
                ZipFile zip = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(INPUT_RESOURCE_DIR)) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        if (!Util.acceptedFilter(entryName, filter)) {
                            continue;
                        }
                        InputStream entryStream = zip.getInputStream(entry);
                        String newEntryName = entryName.substring(INPUT_RESOURCE_DIR.length());
                        File f = createFileFromStream(entryStream, project.getBasedir().getAbsolutePath() + OUTPUT_RESOURCE_DIR + newEntryName);
                        yangFiles.add(f);

                        resources.add(entryStream);
                    }
                }

                resources.add(zip);
            }
        } catch (Exception e) {
            getLog().warn("Exception while loading external yang files.", e);
        }
        return yangFiles;
    }

    private File createFileFromStream(InputStream is, String absoluteName) throws IOException {
        File f = new File(absoluteName);
        if(!f.exists()) {
            f.getParentFile().mkdirs();
        }
        f.createNewFile();

        FileOutputStream fos = new FileOutputStream(f);
        IOUtils.copy(is, fos);
        return f;
    }

    /**
     * Instantiate provider from class and call required method
     */
    private void provideResourcesWithOneProvider(Collection<File> yangFiles,
            ResourceProviderArg resourceProvider)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {

        resourceProvider.check();

        ResourceGenerator g = Util.getInstance(
                resourceProvider.getResourceProviderClass(),
                ResourceGenerator.class);
        getLog().info(
                Util.message("Resource provider instantiated from %s",
                        LOG_PREFIX, resourceProvider.getResourceProviderClass()));

        g.generateResourceFiles(yangFiles, resourceProvider.getOutputBaseDir());
        getLog().info(
                Util.message("Resource provider %s call successful",
                        LOG_PREFIX, resourceProvider.getResourceProviderClass()));
    }

    /**
     * Call generate on every generator from plugin configuration
     */
    private void generateSources(SchemaContext context)
            throws MojoFailureException {
        if (codeGenerators.length == 0) {
            getLog().warn(
                    Util.message("No code generators provided", LOG_PREFIX));
            return;
        }

        Map<String, String> thrown = Maps.newHashMap();
        for (CodeGeneratorArg codeGenerator : codeGenerators) {
            try {
                generateSourcesWithOneGenerator(context, codeGenerator);
            } catch (Exception e) {
                // try other generators, exception will be thrown after
                getLog().error(
                        Util.message(
                                "Unable to generate sources with %s generator",
                                LOG_PREFIX,
                                codeGenerator.getCodeGeneratorClass()), e);
                thrown.put(codeGenerator.getCodeGeneratorClass(), e.getClass()
                        .getCanonicalName());
            }
        }

        if (!thrown.isEmpty()) {
            String message = Util
                    .message(
                            "One or more code generators failed, including failed list(generatorClass=exception) %s",
                            LOG_PREFIX, thrown.toString());
            getLog().error(message);
            throw new MojoFailureException(message);
        }
    }

    /**
     * Instantiate generator from class and call required method
     */
    private void generateSourcesWithOneGenerator(SchemaContext context,
            CodeGeneratorArg codeGeneratorCfg) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IOException {

        codeGeneratorCfg.check();

        CodeGenerator g = Util.getInstance(
                codeGeneratorCfg.getCodeGeneratorClass(), CodeGenerator.class);
        getLog().info(
                Util.message("Code generator instantiated from %s", LOG_PREFIX,
                        codeGeneratorCfg.getCodeGeneratorClass()));

        File outputDir = codeGeneratorCfg.getOutputBaseDir();
        if (project != null && outputDir != null) {
            project.addCompileSourceRoot(outputDir.getPath());
        }
        Collection<File> generated = g.generateSources(context, outputDir);
        getLog().info(
                Util.message("Sources generated by %s: %s", LOG_PREFIX,
                        codeGeneratorCfg.getCodeGeneratorClass(), generated));
    }

    /**
     * Collection of resources which should be closed after use.
     */
    private final List<Closeable> resources = new ArrayList<Closeable>();

    /**
     * Search for yang files in dependent projects.
     *
     * @return files found as List of InputStream
     */
    private List<InputStream> getFilesFromDependenciesAsStream() {
        final List<InputStream> yangsFromDependencies = new ArrayList<InputStream>();
        try {
            List<File> filesOnCp = Util.getClassPath(project);

            List<String> filter = Lists.newArrayList(".yang");
            for (File file : filesOnCp) {
                ZipFile zip = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if(entryName.startsWith(INPUT_RESOURCE_DIR)) {
                        if(entry.isDirectory()) {
                            continue;
                        }
                        if (!Util.acceptedFilter(entryName, filter)) {
                            continue;
                        }

                        InputStream entryStream = zip.getInputStream(entry);
                        yangsFromDependencies.add(entryStream);
                        resources.add(entryStream);
                    }

                }
                resources.add(zip);
            }
        } catch (Exception e) {
            getLog().warn("Exception while searching yangs in dependencies", e);
        }
        return yangsFromDependencies;
    }

    /**
     * Internal utility method for closing open resources.
     */
    private void closeResources() {
        for (Closeable resource : resources) {
            try {
                resource.close();
            } catch (IOException e) {
                getLog().warn("Failed to close resources: "+ resource, e);
            }
        }
    }

}
