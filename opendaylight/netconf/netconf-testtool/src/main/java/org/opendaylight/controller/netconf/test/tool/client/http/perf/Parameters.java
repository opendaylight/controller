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
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class Parameters {

    @Arg(dest = "ip")
    public String ip;

    @Arg(dest = "port")
    public int port;

    @Arg(dest = "edit-count")
    public int editCount;

    @Arg(dest = "edit-content")
    public File editContent;

    @Arg(dest = "exi")
    public boolean exi;

    @Arg(dest = "async")
    public boolean async;

    @Arg(dest = "thread-amount")
    public int threadAmount;

    @Arg(dest = "same-device")
    public boolean sameDevice;

    @Arg(dest = "device-port-range-start")
    public int devicePortRangeStart;

    static ArgumentParser getParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("netconf stress client");

        parser.description("Netconf stress client");

        parser.addArgument("--ip")
                .type(String.class)
                .setDefault("127.0.0.1")
                .type(String.class)
                .help("Restconf server IP")
                .dest("ip");

        parser.addArgument("--port")
                .type(Integer.class)
                .setDefault(8181)
                .type(Integer.class)
                .help("Restconf server port")
                .dest("port");

        parser.addArgument("--edits")
                .type(Integer.class)
                .setDefault(50000)
                .type(Integer.class)
                .help("Netconf edit rpcs to be sent")
                .dest("edit-count");

        parser.addArgument("--edit-content")
                .type(File.class)
                .setDefault(new File("edit.txt"))
                .type(File.class)
                .dest("edit-content");

        parser.addArgument("--exi")
                .type(Boolean.class)
                .setDefault(false)
                .dest("exi");

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
                .dest("same-device");

        parser.addArgument("--device-port-range-start")
                .type(Integer.class)
                .setDefault(17830)
                .dest("device-port-range-start");

        return parser;
    }

    void validate() {
        Preconditions.checkArgument(port > 0, "Port =< 0");
        Preconditions.checkArgument(editCount > 0, "Edit count =< 0");

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
