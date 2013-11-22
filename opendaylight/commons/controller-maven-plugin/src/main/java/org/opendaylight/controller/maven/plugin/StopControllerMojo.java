/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Stop controller
 */
@Mojo( name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST )
public class StopControllerMojo extends AbstractControllerMojo {
    private static final boolean SKIP_STOP = Boolean.getBoolean("skipControllerStop");

    @Override
    public void start() throws MojoExecutionException, MojoFailureException {
        if (SKIP_STOP) {
            getLog().info("Controller STOP is skipped per configuration " +
                    "(-DskipControllerStop=true).");
            return;
        }
        if (canConnect()) {
            List<String> args = new ArrayList<String>();
            args.add("-stop");
            Process proc = invokeScript(args, null);
            try {
                int status = proc.waitFor();
                if (status == 0) {
                    getLog().info("Controller stopped.");
                } else {
                    getLog().error("Error stopping controller. See stdout log for details.");
                }
            } catch (InterruptedException ie) {
                throw new MojoExecutionException("Error stopping controller : " + ie.getMessage());
            }
        } else {
            getLog().info("Controller not running.");
        }
        // cleanup for any hung processes
        killControllers();
    }

}
