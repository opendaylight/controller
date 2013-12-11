/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcessMonitor {
    public static final String SEP = File.pathSeparator;
    public static final boolean WIN =
            System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;



    public void log(String msg) {
        System.out.println("" + msg);
    }

    public List<JavaProcess> getProcesses() {
        return Collections.emptyList();
    }

    public List<JavaProcess> getProcesses(String mainClass, String systemPropertyKey) {
        List<JavaProcess> result = new ArrayList<JavaProcess>();
         for (JavaProcess info : getProcesses()) {
            if (info.getMainClass().equals(mainClass)) {
                if (info.getSystemProperties().containsKey(systemPropertyKey)) {
                    result.add(info);
                }
            }
        }
        return result;
    }

    public int kill(String mainClass) {
        for (JavaProcess info : getProcesses()) {
            if (info.getMainClass().equals(mainClass)) {
                log("Killing process matching class: " + mainClass);
                return kill(info.getPid());
            }
        }
        return -1;
    }

    public static int kill(int pid)  {
        String cmd = WIN ? "TASKKILL /F /PID " + pid : "kill -SIGTERM " + pid;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static ProcessMonitor load() {
        // load the providers dynamically to allow error handling
        ProcessMonitor pm = load("org.opendaylight.controller.maven.plugin.util.VMProcessMonitor");
        if (pm == null) {
            pm = load("org.opendaylight.controller.maven.plugin.util.JpsProcessMonitor");
        }
        return (pm == null ? new ProcessMonitor() : pm);
    }

    private static ProcessMonitor load(String clz) {
        try {
            Class c = Class.forName(clz);
            return (ProcessMonitor) c.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // simple driver for basic manual testing
    public static void main(String[] args) throws Exception {
        ProcessMonitor pm = ProcessMonitor.load();
        System.out.println("==== " + pm);
        for (JavaProcess info : pm.getProcesses()) {
            System.out.println(info.toString());
        }
        pm.kill("Foo");
        System.out.println("==== controller processses ");
        for (JavaProcess info : pm.getProcesses(
                "org.eclipse.equinox.launcher.Main", "opendaylight.controller"))
        {
            System.out.println(info.toString());
            pm.kill(info.getPid());
        }
    }

}
