/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.maven.plugin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses JPS tool to monitor java local processes
 */
public class JpsProcessMonitor extends ProcessMonitor {
    private final String jpsTool;

    public JpsProcessMonitor() {
        String jh = System.getProperty("java.home");
        File jps = new File(jh + SEP + "bin" + SEP + "jps");
        if (!jps.exists()) {
            // try one dir above
            jps = new File(jh + SEP + ".." + SEP + "bin" + SEP + "jps");
            if (!jps.exists()) {
                throw new IllegalStateException("jps tool cannot be located.");
            }
        }
        jpsTool = jps.getAbsolutePath();
    }

    @Override
    public List<JavaProcess> getProcesses() {
        if (jpsTool == null) return super.getProcesses();
        List<JavaProcess> jvms = new ArrayList<JavaProcess>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(new String[] { jpsTool, "-mlvV"} );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = null;
            while ( (line = br.readLine()) != null) {
                JavaProcess j = parseLine(line);
                if (j != null) jvms.add(j);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jvms;
    }

    public static JavaProcess parseLine(String line) {
        String[] tokens = line.split("\\s");
        if (tokens.length < 2) {
            System.out.println("Unable to parse line: " + line);
            return null;
        }
        int idx = 0;
        int pid = Integer.parseInt(tokens[idx++]);
        String mainClass = "";
        if (!tokens[idx].startsWith("-")) {
            mainClass = tokens[idx++];
        }
        JavaProcess proc = new JavaProcess(pid, mainClass);
        for (int i=idx; i<tokens.length; i++) {
            if (tokens[i].startsWith("-D")) {
                proc.setSystemProperty(tokens[i]);
            }
        }
        return proc;
    }

}
