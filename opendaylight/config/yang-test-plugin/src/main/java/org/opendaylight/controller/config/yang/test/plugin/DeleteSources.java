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

import java.io.File;

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
        // We now generate the persistent source files under src/main/yang-gen-config
        // so nothing currently nothing needs to be done here to cleanup.
    }
}
