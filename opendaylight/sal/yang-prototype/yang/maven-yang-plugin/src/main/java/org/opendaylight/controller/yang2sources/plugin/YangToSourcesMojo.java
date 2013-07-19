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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

import com.google.common.annotations.VisibleForTesting;

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

    /**
     * Classes implementing {@link CodeGenerator} interface. An instance will be
     * created out of every class using default constructor. Method {@link
     * CodeGenerator#generateSources(SchemaContext, File, Set<String>
     * yangModulesNames)} will be called on every instance.
     */
    @Parameter(required = false)
    private CodeGeneratorArg[] codeGenerators;

    /**
     * Source directory that will be recursively searched for yang files (ending
     * with .yang suffix).
     */
    @Parameter(required = false)
    private String yangFilesRootDir; // defaults to ${basedir}/src/main/yang

    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "inspectDependencies", required = true, readonly = true)
    private boolean inspectDependencies;

    private YangToSourcesProcessor yangToSourcesProcessor;

    public YangToSourcesMojo() {

    }

    @VisibleForTesting
    YangToSourcesMojo(YangToSourcesProcessor processor) {
        this.yangToSourcesProcessor = processor;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (yangToSourcesProcessor == null) {
            List<CodeGeneratorArg> codeGeneratorArgs = processCodeGenerators(codeGenerators);

            // defaults to ${basedir}/src/main/yang
            File yangFilesRootFile = processYangFilesRootDir(yangFilesRootDir,
                    project.getBasedir());

            yangToSourcesProcessor = new YangToSourcesProcessor(getLog(),
                    yangFilesRootFile, codeGeneratorArgs, project,
                    inspectDependencies);
        }
        yangToSourcesProcessor.execute();
    }

    private static List<CodeGeneratorArg> processCodeGenerators(
            CodeGeneratorArg[] codeGenerators) {
        List<CodeGeneratorArg> codeGeneratorArgs;
        if (codeGenerators == null) {
            codeGeneratorArgs = Collections.emptyList();
        } else {
            codeGeneratorArgs = Arrays.asList(codeGenerators);
        }
        return codeGeneratorArgs;
    }

    private static File processYangFilesRootDir(String yangFilesRootDir,
            File baseDir) {
        File yangFilesRootFile;
        if (yangFilesRootDir == null) {
            yangFilesRootFile = new File(baseDir, "src" + File.separator
                    + "main" + File.separator + "yang");
        } else {
            File file = new File(yangFilesRootDir);
            if (file.isAbsolute()) {
                yangFilesRootFile = file;
            } else {
                yangFilesRootFile = new File(baseDir, file.getPath());
            }
        }
        return yangFilesRootFile;
    }
}
