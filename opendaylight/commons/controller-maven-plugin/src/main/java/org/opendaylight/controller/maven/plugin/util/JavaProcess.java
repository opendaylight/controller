
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.maven.plugin.util;

import java.util.Properties;

public class JavaProcess {
    private final int pid;
    private final String mainClass;
    private final Properties systemProperties = new Properties();

    public JavaProcess(int id, String cls) {
        this.pid = id;
        this.mainClass = cls;
    }

    public void setSystemProperties(String line) {
        if (line == null || line.length() == 0) return;
        String[] tokens = line.split("\\s");
        for (String t : tokens) setSystemProperty(t);
    }

    public void setSystemProperty(String line) {
        if (line.startsWith("-D")) {
            int x = line.indexOf('=');
            if (x > -1) {
                systemProperties.put(line.substring(2, x), line.substring(x+1));
            } else {
                systemProperties.put(line.substring(2), "");
            }
        }
    }

    public int getPid() {
        return pid;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    @Override
    public String toString() {
        return "pid:" + pid + " class:" + mainClass +
                " system-properties:" + systemProperties.toString();
    }
}
