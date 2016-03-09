/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.http.perf;

import com.google.common.base.Preconditions;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class Parameters {

    @Arg(dest = "ip")
    public String ip;

    @Arg(dest = "port")
    public int port;

    @Arg(dest = "destination")
    public String destination;

    @Arg(dest = "edit-count")
    public int editCount;

    @Arg(dest = "edit-content")
    public File editContent;

    @Arg(dest = "async")
    public boolean async;

    @Arg(dest = "thread-amount")
    public int threadAmount;

    @Arg(dest = "same-device")
    public boolean sameDevice;

    @Arg(dest = "device-port-range-start")
    public int devicePortRangeStart;

    @Arg(dest = "throttle")
    public int throttle;

    @Arg(dest = "auth")
    public ArrayList<String> auth;

    @Arg(dest = "timeout")
    public long timeout;

    static ArgumentParser getParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("netconf stress client");

        parser.description("Netconf stress client");

        parser.addArgument("--ip")
                .type(String.class)
                .setDefault("127.0.0.1")
                .help("Restconf server IP")
                .dest("ip");

        parser.addArgument("--port")
                .type(Integer.class)
                .setDefault(8181)
                .help("Restconf server port")
                .dest("port");

        parser.addArgument("--destination")
                .type(String.class)
                .setDefault("/restconf/config/network-topology:network-topology/topology/topology-netconf/node/" +
                        "{DEVICE_PORT}-sim-device/yang-ext:mount/cisco-vpp:vpp/bridge-domains/bridge-domain/a")
                .help("Destination to send the requests to after the ip:port part of the uri. " +
                        "Use {DEVICE_PORT} tag to use the device-port-range-start argument")
                .dest("destination");

        parser.addArgument("--edits")
                .type(Integer.class)
                .setDefault(50000)
                .help("Amount requests to be sent")
                .dest("edit-count");

        parser.addArgument("--edit-content")
                .type(File.class)
                .setDefault(new File("edit.txt"))
                .dest("edit-content");

        parser.addArgument("--async-requests")
                .type(Boolean.class)
                .setDefault(true)
                .dest("async");

        parser.addArgument("--thread-amount")
                .type(Integer.class)
                .setDefault(1)
                .dest("thread-amount");

        parser.addArgument("--same-device")
                .type(Boolean.class)
                .setDefault(true)
                .help("If true, every thread edits the device at the first port. If false, n-th thread edits device at n-th port.")
                .dest("same-device");

        parser.addArgument("--device-port-range-start")
                .type(Integer.class)
                .setDefault(17830)
                .dest("device-port-range-start");

        parser.addArgument("--throttle")
                .type(Integer.class)
                .setDefault(5000)
                .help("Maximum amount of async requests that can be open at a time, " +
                        "with mutltiple threads this gets divided among all threads")
                .dest("throttle");

        parser.addArgument("--auth")
                .nargs(2)
                .help("Username and password for HTTP basic authentication in order username password.")
                .dest("auth");

        parser.addArgument("--timeout")
                .type(Long.class)
                .setDefault(5)
                .help("Maximum time in minutes to wait for finishing all requests.")
                .dest("timeout");

        return parser;
    }

    void validate() {
        Preconditions.checkArgument(port > 0, "Port =< 0");
        Preconditions.checkArgument(editCount > 0, "Edit count =< 0");
        Preconditions.checkArgument(timeout > 0, "Timeout =< 0");

        Preconditions.checkArgument(editContent.exists(), "Edit content file missing");
        Preconditions.checkArgument(editContent.isDirectory() == false, "Edit content file is a dir");
        Preconditions.checkArgument(editContent.canRead(), "Edit content file is unreadable");
        // TODO validate
    }

    public InetSocketAddress getInetAddress() {
        try {
            return new InetSocketAddress(InetAddress.getByName(ip), port);
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unknown ip", e);
        }
    }
}
