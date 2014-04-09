/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Add implementation code from stub.txt
 *
 * @goal process-sources
 *
 * @phase process-sources
 *
 */
public class ProcessSources extends AbstractMojo{
    /**
     * @parameter expression="${project.build.sourceDirectory}"
     * @readOnly
     * @required
     */
    private File directory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (directory == null || !directory.exists()) {
            super.getLog().error("Directory does not exists.");
        }
        File sourceDirectory = new File(directory.getPath() + Util.replaceDots(".org.opendaylight.controller.config.yang.test.impl"));
        if (!sourceDirectory.exists()) {
            super.getLog().error("Source directory does not exists " + sourceDirectory.getPath());
        }

        File[] sourceFiles = sourceDirectory.listFiles();
        for (File sourceFile: sourceFiles) {
            if(sourceFile.getName().endsWith("Module.java") || sourceFile.getName().endsWith("ModuleFactory.java")) {
                File stubFile = new File(sourceFile.getPath().replace(".java", "Stub.txt"));
                if (stubFile.exists()) {
                    try {
                        rewrite(sourceFile, FileUtils.readFileToString(stubFile));
                    } catch (IOException e) {
                        getLog().error("Error while reading/writing to files.", e);
                    }
                }
            }
        }
    }

    private static void rewrite(File sourceFile, String replaceTODOWith) throws IOException {
        String source = FileUtils.readFileToString(sourceFile);
        String target = Pattern.compile("^.*TODO.*\n.*throw new java.lang.UnsupportedOperationException.*$", Pattern.MULTILINE).matcher(source).replaceFirst(replaceTODOWith);
        FileUtils.write(sourceFile, target);
    }
}
