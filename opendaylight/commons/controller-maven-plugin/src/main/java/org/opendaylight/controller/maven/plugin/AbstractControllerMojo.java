/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import org.opendaylight.controller.maven.plugin.util.JavaProcess;
import org.opendaylight.controller.maven.plugin.util.ProcessMonitor;

/**
 * Base controller mojo which handles common operations
 */
public abstract class AbstractControllerMojo extends AbstractMojo {
    public static final String OS_NAME = System.getProperty("os.name");
    public static final boolean WIN = OS_NAME.toUpperCase().contains("WINDOWS");
    public static final String JAVA_HOME = "JAVA_HOME";
    public static final boolean skip = Boolean.getBoolean("controller.startup.skip");
    public static final String MAIN_CLASS = "org.eclipse.equinox.launcher.Main";
    public static final String CTRL_PROP = "opendaylight.controller";

    /**
     * The home directory where controller is installed
     */
    @Parameter( required = false )
    protected File controllerHome;

    /**
     * The address on which controller is listening
     */
    @Parameter( defaultValue = "localhost")
    protected String controllerHost;

    /**
     * The admin web port
     */
    @Parameter( defaultValue = "8080")
    protected int controllerWebPort;

    /**
     * The openflow port
     */
    @Parameter( defaultValue = "6633")
    protected int controllerOFPort;

    /**
     * Additional environment variables passed when starting the controller
     * process.
     */
    @Parameter(required = false)
    protected Properties controllerShellVariables;

    /**
     * The script name to invoke
     */
    @Parameter(required = false)
    protected String controllerStartScriptName;

    /**
     * The username
     */
    @Parameter(required = false)
    protected String controllerUsername;

    /**
     * The password
     */
    @Parameter(required = false)
    protected String controllerPassword;

    /**
     * pidFile location
     */
    @Parameter(required = false)
    protected File pidFile;

    protected final ProcessMonitor procMon = ProcessMonitor.load();

    public abstract void start() throws MojoExecutionException, MojoFailureException;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) return;
        validateArgs();
        start();
    }

    protected URL getWebUrl() {
      try {
        return new URL("http", controllerHost, controllerWebPort, "/");
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException(
            "controller host:port is Malformed: " + controllerHost + " " + controllerWebPort, e);
      }

    }

    protected void validateArgs() throws IllegalArgumentException {
        // System property and environment variable override the default setting
        String odlHome = System.getProperty("controllerHome");
        if (odlHome != null) {
          controllerHome = new File(odlHome);
        }
        if (controllerHome == null) {
            getLog().error("controllerHome cannot be determined from controllerHome "
                + "property or ONE_HOME env variable");
            throw new IllegalArgumentException("controllerHome cannot be determined.");
        }
        if (!controllerHome.exists()) {
            throw new IllegalArgumentException(
                    "controllerHome does not exist: " + controllerHome);
        }
       if (controllerUsername == null) {
            controllerUsername = System.getProperty("controllerUsername");
        }
        if (controllerPassword == null) {
            controllerPassword= System.getProperty("controllerPassword");
        }
        URL u = getWebUrl();
        getLog().info("Controller Home       : " + controllerHome);
        getLog().info("Controller Url        : " + u);
        getLog().info("Controller credentials: " + controllerUsername
                + "/" + controllerPassword);
    }

    protected Process invokeScript(List<String> args, String log)
            throws MojoFailureException, MojoExecutionException
    {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<String>();
        cmd.add(getScript());
        if (args != null) {
            for (String s : args) {
                // on windows args containing equals symbols need to be quoted
                if (WIN && s.contains("=") && !s.startsWith("\"")) {
                  cmd.add("\"" + s + "\"");
                } else {
                  cmd.add(s);
                }
            }
        }
        pb.command(cmd);
        pb.directory(controllerHome);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        Map<String,String> env = pb.environment();
        if (controllerShellVariables != null) {
            for (Enumeration e = controllerShellVariables.propertyNames(); e.hasMoreElements();) {
                String n = (String) e.nextElement();
                env.put(n, controllerShellVariables.getProperty(n));
            }
        }
        String jh = env.get(JAVA_HOME);
        if (jh == null) env.put(JAVA_HOME, System.getProperty("java.home"));
        try {
            getLog().info("Invoking process " + pb.command());
            return pb.start();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private String getScript() throws MojoFailureException {
        File script = null;
        if (controllerStartScriptName != null && !"".equals(controllerStartScriptName) ) {
            script = new File(controllerStartScriptName);
            if (!script.exists()) {
                // try relative path
                script = new File(controllerHome, controllerStartScriptName);
            }
            if (script.exists()) return script.getAbsolutePath();
            throw new MojoFailureException("Script not found: " + controllerStartScriptName);
        }
        // try default
        script = new File(controllerHome, "run." + (WIN ? "bat" : "sh") );
        if (script.exists()) return script.getAbsolutePath();
        throw new MojoFailureException("Cannot find a default script to launch.");
    }

    protected boolean canConnect() {
        try {
            URL url = getWebUrl();
            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            return (con.getResponseCode() > 0);
        } catch (IOException e) {
            return false;
        }
    }

    public void killControllers() {
        getLog().info("Checking environment for stray processes.");
        List<JavaProcess> jvms = procMon.getProcesses(MAIN_CLASS, CTRL_PROP);
        for (JavaProcess j : jvms) {
            getLog().info("Killing running process: " + j);
            ProcessMonitor.kill(j.getPid());
        }
        // cleanup pid files
        getLog().info("Checking left over pid file: " + pidFile);
        if (pidFile != null && pidFile.exists()) {
            getLog().info("Cleaning up pid file : " + pidFile);
            pidFile.delete();
        }
    }

    public boolean isControllerRunning() {
        return !procMon.getProcesses(MAIN_CLASS, CTRL_PROP).isEmpty();
    }

}
