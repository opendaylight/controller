/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

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
        String header = "";
        try {
            header = Util.loadHeader();
        } catch (IOException e) {
           super.getLog().error("Header.txt not found.");
        }
        File[] sourceFiles = sourceDirectory.listFiles();
        for (File sourceFile: sourceFiles) {
            if(sourceFile.getName().endsWith("Module.java") || sourceFile.getName().endsWith("ModuleFactory.java")) {
                File stubFile = new File(sourceFile.getPath().replace(".java", "Stub.txt"));
                String stubLines = null;
                try {
                    if (stubFile.exists()) {
                        stubLines = Util.loadStubFile(stubFile.getPath());
                    }

                    InputStream javaIn = new FileInputStream(sourceFile.getPath());
                    BufferedReader javaBuf = new BufferedReader(new InputStreamReader(javaIn));
                    StringBuffer output = new StringBuffer();
                    String line = javaBuf.readLine();
                    boolean writeLine = false;
                    while ((line = javaBuf.readLine()) != null) {
                        if(!writeLine && line.contains("*/")) {
                            line = header;
                            writeLine = true;
                        } else {
                            if (line.contains("TODO")) {
                                writeLine = false;
                            } else {
                                if (stubLines != null && line.contains("throw new")) {
                                    line = stubLines.toString();
                                    writeLine = true;
                                }
                            }
                        }
                        if(writeLine) {
                            output.append(line).append(System.lineSeparator());
                        }
                    }
                    javaBuf.close();

                    OutputStream javaOut = new FileOutputStream(sourceFile.getPath());
                    javaOut.write(output.toString().getBytes());
                    javaOut.close();
                } catch (IOException e) {
                    getLog().error("Error while reading/writing to files.", e);
                }

            }
        }
    }
}
