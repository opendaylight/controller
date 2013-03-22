
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northboundtest.unittestsuite.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * This java class provides the osgi console with the commands for running the unit test scripts for the API3
 *
 *
 *
 */
public class API3UnitTest implements CommandProvider {
    private static final String python = "/usr/bin/python";

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---API3 Unit Test---\n");
        help
                .append("\t api3ut             - run the python script for the specified northbound module\n");
        help.append("\t GET <uri>");
        help.append("\t PUT <uri>  data1==x1 data2==x2 ...");
        help.append("\t POST <uri>  data1==x1 data2==x2 ...");
        help.append("\t DELETE <uri>");
        return help.toString();
    }

    public void _api3ut(CommandInterpreter ci) {
        boolean custom = false;
        String target = null;
        String module = null;

        module = ci.nextArgument();
        if (module == null) {
            printUsage(ci);
            return;
        }

        if (module.equals("custom")) {
            target = ci.nextArgument();
            custom = true;
        } else if (module.equals("flows")) {
            target = "flowsUnitTest.py";
        } else if (module.equals("subnets")) {
            target = "subnetsUnitTest.py";
        } else if (module.equals("hosts")) {
            target = "hostsUnitTest.py";
        } else if (module.equals("slices")) {
            target = "slicesUnitTest.py";
        } else if (module.equals("tif")) {
            target = "tifUnitTest.py";
        } else {
            ci.println("ERROR: Coming soon");
        }

        if (target != null) {
            executeScript(target, custom);
        }
    }

    private void printUsage(CommandInterpreter ci) {
        ci.println("Usage: api3ut [<module> | custom <target>]");
        ci
                .println("<module>: [flows, hosts, subnets, slices, tif] (You need python-httplib2 installed)");
        ci.println("<target>: your linux script (w/ absolute path)");
    }

    private void printStream(InputStream stream) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    public void executeScript(String target, boolean custom)
            throws RuntimeException {
        String script = (custom) ? target : "SCRIPTS/python/" + target;
        try {
            Runtime runTime = Runtime.getRuntime();
            Process process = runTime.exec(python + " " + script);
            printStream(process.getInputStream());
            printStream(process.getErrorStream());
        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
    }

    public void _GET(CommandInterpreter ci) {
        parseRestRequest("GET", ci);
    }

    public void _PUT(CommandInterpreter ci) {
        parseRestRequest("PUT", ci);
    }

    public void _DELETE(CommandInterpreter ci) {
        parseRestRequest("DELETE", ci);
    }

    public void _POST(CommandInterpreter ci) {
        parseRestRequest("POST", ci);
    }

    private void parseRestRequest(String action, CommandInterpreter ci) {
        String uri, resource;
        StringBuffer resources = new StringBuffer(" ");

        uri = ci.nextArgument();
        if (uri == null) {
            printRestUsage(ci);
            return;
        }

        resource = ci.nextArgument();
        while (resource != null) {
            resources.append(resource);
            resources.append(" ");
            resource = ci.nextArgument();
        }

        executeRestCall(action, uri, resources.toString());

    }

    private void executeRestCall(String action, String uri, String resources) {
        String script = "SCRIPTS/python/rest_call.py";

        try {
            Runtime runTime = Runtime.getRuntime();
            Process process = runTime.exec(python + " " + script + " " + action
                    + " " + uri + " " + resources);
            printStream(process.getInputStream());
            printStream(process.getErrorStream());
        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
    }

    private void printRestUsage(CommandInterpreter ci) {
        ci.println("Usage: GET/PUT/POST/DELETE <uri>  [<resources>]");
        ci.println("<uri>: ex: slices/red or slices/red/flowspecs");
        ci
                .println("<resources>: resource==<value>,... ex: switchId==2 port==3-7");
    }
}
