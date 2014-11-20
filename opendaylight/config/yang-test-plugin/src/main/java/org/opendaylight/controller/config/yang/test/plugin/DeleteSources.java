/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.plugin;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Delete all Module/ModuleFactory sources
 *
 * @goal delete-sources
 *
 * @phase initialize
 */
public class DeleteSources extends AbstractMojo{
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
        if (sourceDirectory == null || !sourceDirectory.exists()) {
            super.getLog().error(String.format("Source directory does not exists %s", sourceDirectory.getPath()));
        }
        File[] sourceFiles = sourceDirectory.listFiles();
        for (File sourceFile: sourceFiles) {
            if(sourceFile.getName().endsWith("Module.java") || sourceFile.getName().endsWith("ModuleFactory.java")) {
                super.getLog().debug(String.format("Source file deleted: %s", sourceFile.getName()));
                sourceFile.delete();
            }
        }
    }
}
