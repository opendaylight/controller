/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Starts the controller
 */
@Mojo( name = "run", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST )
public class StartControllerMojo extends AbstractControllerMojo {
    public static final String REDIRECT_LOG = "controller.out";

    /**
     * The timeout value for starting the controller. Defaults to 60 secs
     */
    @Parameter(defaultValue = "60")
    public int timeoutSecs = 60;

    /**
     * The startArgs for starting the controller
     */
    @Parameter(required = false)
    protected List<String> startArgs = new ArrayList<String>();

    /**
     * The time to wait after successfully connecting to the controller and
     * before returning from execution.
     */
    @Parameter(required = false)
    protected int warmupTimeSecs = 10;


    @Override
    public void start() throws MojoExecutionException, MojoFailureException {
        killControllers();
        // if we can still connect to a controller, bail out
        if (canConnect()) {
            getLog().error("A controller is already running. Shutdown and retry.");
            throw new MojoFailureException("Controller is already running.");
        }
        startArgs.add("-D" + CTRL_PROP);
        Process process = invokeScript(startArgs, REDIRECT_LOG);
        getLog().info("Controller starting... (waiting for open ports)");
        try {
            waitForListening(process);
            getLog().info("Controller port open. Waiting for warmup: "
                    + warmupTimeSecs);
            Thread.sleep(warmupTimeSecs*1000);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
        getLog().info("Controller started successfully.");
    }

    protected boolean waitForListening(Process process)
            throws MalformedURLException, InterruptedException, MojoExecutionException
    {
        long timeElapsedMillis = 0L;
        long sleepTimeMillis = 2000L; // 2 secs
        long timeoutMillis = timeoutSecs * 1000;

        while (timeElapsedMillis < timeoutMillis) {
            long timeRemaining = timeoutMillis - timeElapsedMillis;
            sleepTimeMillis *= 2;
            long toSleep = (sleepTimeMillis > timeRemaining)
                    ? timeRemaining : sleepTimeMillis;
            Thread.sleep(toSleep);
            timeElapsedMillis += toSleep;
            if (canConnect()) {
                return true;
            }
            if (!isControllerRunning()) {
                throw new MojoExecutionException("Process seems to have exited prematurely.");
            }
        }
        return false;
    }
}
