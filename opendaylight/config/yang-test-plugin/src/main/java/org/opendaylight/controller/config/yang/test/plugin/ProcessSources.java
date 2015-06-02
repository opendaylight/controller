/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.plugin;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
            super.getLog().error(String.format("Source directory does not exists %s", sourceDirectory.getPath()));
        }

        File[] sourceFiles = sourceDirectory.listFiles();
        for (File sourceFile: sourceFiles) {
            if (sourceFile.getName().endsWith(".java")) {
                String sourceContent;
                try {
                    sourceContent = Files.toString(sourceFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    getLog().error(String.format("Cannot read %s", sourceFile.getAbsolutePath()), e);
                    continue;
                }
                if (sourceFile.getName().endsWith("Module.java") || sourceFile.getName().endsWith("ModuleFactory.java")) {
                    File stubFile = new File(sourceFile.getPath().replace(".java", "Stub.txt"));
                    if (stubFile.exists()) {
                        String stubContent = null;
                        try {
                            stubContent = Files.toString(stubFile, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            getLog().error(String.format("Cannot read %s", stubFile.getAbsolutePath()), e);
                        }
                        if (stubContent != null) {
                            sourceContent = rewriteStub(sourceContent, stubContent);
                        }
                    }
                }
                // remove copyright headers as they can contain timestamp
                sourceContent = removeCopyrights(sourceContent);

                // replace the file content
                try {
                    Files.write(sourceContent, sourceFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    getLog().error(String.format("Cannot write %s", sourceFile.getAbsolutePath()), e);
                }
            }

        }
    }

    private static Pattern MULTILINE_COMMENT_PATTERN = Pattern.compile("/\\*.*\\*/", Pattern.MULTILINE | Pattern.DOTALL);
    private static String removeCopyrights(String source) {
        String target = MULTILINE_COMMENT_PATTERN.matcher(source).replaceAll("\n");
        //FileUtils.write(sourceFile, target);
        return target;
    }

    private static Pattern UNSUPPORTED_OP_PATTERN = Pattern.compile("^.*TODO.*\n.*throw new java.lang.UnsupportedOperationException.*$", Pattern.MULTILINE);

    private static String rewriteStub(String source, String replaceTODOWith) {
        String target = UNSUPPORTED_OP_PATTERN.matcher(source).replaceFirst(replaceTODOWith);
        return target;
    }
}
