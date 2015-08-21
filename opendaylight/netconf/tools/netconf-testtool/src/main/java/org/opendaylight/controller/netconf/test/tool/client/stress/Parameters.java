/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.client.stress;

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

    @Arg(dest = "edit-batch-size")
    public int editBatchSize;

    @Arg(dest = "candidate-datastore")
    public boolean candidateDatastore;

    @Arg(dest = "debug")
    public boolean debug;

    @Arg(dest = "legacy-framing")
    public boolean legacyFraming;

    @Arg(dest = "exi")
    public boolean exi;

    @Arg(dest = "async")
    public boolean async;

    @Arg(dest = "ssh")
    public boolean ssh;

    @Arg(dest = "username")
    public String username;

    @Arg(dest = "password")
    public String password;

    @Arg(dest = "msg-timeout")
    public long msgTimeout;

    @Arg(dest = "tcp-header")
    public String tcpHeader;

    @Arg(dest = "thread-amount")
    public int threadAmount;

    static ArgumentParser getParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("netconf stress client");

        parser.description("Netconf stress client");

        parser.addArgument("--ip")
                .type(String.class)
                .setDefault("127.0.0.1")
                .type(String.class)
                .help("Netconf server IP")
                .dest("ip");

        parser.addArgument("--port")
                .type(Integer.class)
                .setDefault(2830)
                .type(Integer.class)
                .help("Netconf server port")
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

        parser.addArgument("--edit-batch-size")
                .type(Integer.class)
                .required(false)
                .setDefault(-1)
                .dest("edit-batch-size");

        parser.addArgument("--candidate-datastore")
                .type(Boolean.class)
                .required(false)
                .setDefault(true)
                .help("Edit candidate or running datastore. Defaults to candidate.")
                .dest("candidate-datastore");

        parser.addArgument("--debug")
                .type(Boolean.class)
                .setDefault(false)
                .help("Whether to use debug log level instead of INFO")
                .dest("debug");

        parser.addArgument("--legacy-framing")
                .type(Boolean.class)
                .setDefault(false)
                .dest("legacy-framing");

        parser.addArgument("--exi")
                .type(Boolean.class)
                .setDefault(false)
                .dest("exi");

        parser.addArgument("--async-requests")
                .type(Boolean.class)
                .setDefault(true)
                .dest("async");

        parser.addArgument("--msg-timeout")
                .type(Integer.class)
                .setDefault(60)
                .dest("msg-timeout");

        parser.addArgument("--ssh")
                .type(Boolean.class)
                .setDefault(false)
                .dest("ssh");

        parser.addArgument("--username")
                .type(String.class)
                .setDefault("admin")
                .dest("username");

        parser.addArgument("--password")
                .type(String.class)
                .setDefault("admin")
                .dest("password");

        parser.addArgument("--tcp-header")
                .type(String.class)
                .required(false)
                .dest("tcp-header");

        parser.addArgument("--thread-amount")
                .type(Integer.class)
                .setDefault(1)
                .dest("thread-amount");

        // TODO add get-config option instead of edit + commit
        // TODO different edit config content

        return parser;
    }

    void validate() {
        Preconditions.checkArgument(port > 0, "Port =< 0");
        Preconditions.checkArgument(editCount > 0, "Edit count =< 0");
        if (editBatchSize == -1) {
            editBatchSize = editCount;
        } else {
            Preconditions.checkArgument(editBatchSize <= editCount, "Edit count =< 0");
        }

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
