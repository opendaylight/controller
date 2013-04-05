/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.parser.impl.YangModelParserImpl;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class YangToSourcesMojo extends AbstractMojo {

    private static final String LOG_PREFIX = "yang-to-sources:";

    @Parameter(required = true)
    private CodeGeneratorArg[] codeGenerators;

    @Parameter(required = true)
    private String yangFilesRootDir;

    private final YangModelParser parser;

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
        parser = new YangModelParserImpl();
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
            String[] yangFiles = Util.listFilesAsArrayOfPaths(yangFilesRootDir);

            if (yangFiles.length == 0)
                getLog().warn(
                        Util.message("No %s file found in %s", LOG_PREFIX,
                                Util.YANG_SUFFIX, yangFilesRootDir));
            // TODO only warning or throw exception ?

            Set<Module> parsedYang = parser.parseYangModels(yangFiles);
            SchemaContext resolveSchemaContext = parser
                    .resolveSchemaContext(parsedYang);
            getLog().info(
                    Util.message("%s files parsed from %s", LOG_PREFIX,
                            Util.YANG_SUFFIX, Arrays.toString(yangFiles)));
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
            CodeGeneratorArg codeGenerator) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {

        codeGenerator.check();

        CodeGenerator g = Util.getInstance(
                codeGenerator.getCodeGeneratorClass(), CodeGenerator.class);
        getLog().info(
                Util.message("Code generator instantiated from %s", LOG_PREFIX,
                        codeGenerator.getCodeGeneratorClass()));

        Collection<File> generated = g.generateSources(context,
                codeGenerator.getOutputBaseDir());
        getLog().info(
                Util.message("Sources generated by %s: %s", LOG_PREFIX,
                        codeGenerator.getCodeGeneratorClass(), generated));
    }

}
