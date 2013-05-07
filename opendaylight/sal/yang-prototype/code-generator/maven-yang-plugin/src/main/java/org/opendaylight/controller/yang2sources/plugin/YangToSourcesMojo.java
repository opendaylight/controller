/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

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
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

import com.google.common.annotations.VisibleForTesting;
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

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    private transient final YangModelParser parser;

    @VisibleForTesting
    YangToSourcesMojo(CodeGeneratorArg[] codeGeneratorArgs,
            YangModelParser parser, String yangFilesRootDir) {
        super();
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
    }

    /**
     * Generate {@link SchemaContext} with {@link YangModelParserImpl}
     */
    private SchemaContext processYang() throws MojoExecutionException {
        try {
            Collection<File> yangFiles = Util.listFiles(yangFilesRootDir);

            if (yangFiles.isEmpty()) {
                getLog().warn(
                        Util.message("No %s file found in %s", LOG_PREFIX,
                                Util.YANG_SUFFIX, yangFilesRootDir));
                return null;
            }

            Set<Module> parsedYang = parser
                    .parseYangModels(new ArrayList<File>(yangFiles));
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

}
