/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    static class Params {

        @Arg(dest = "schemas-dir")
        public File schemasDir;

        @Arg(dest = "devices-count")
        public int deviceCount;

        @Arg(dest = "starting-port")
        public int startingPort;

        static ArgumentParser getParser() {
            final ArgumentParser parser = ArgumentParsers.newArgumentParser("netconf testool");
            parser.addArgument("--devices-count")
                    .type(Integer.class)
                    .setDefault(1)
                    .type(Integer.class)
                    .help("Number of simulated netconf devices to spin")
                    .dest("devices-count");

            parser.addArgument("--schemas-dir")
                    .type(File.class)
                    .required(true)
                    .help("Directory containing yang schemas to describe simulated devices")
                    .dest("schemas-dir");


            parser.addArgument("--starting-port")
                    .type(Integer.class)
                    .setDefault(10830)
                    .help("First port for simulated device. Each other device will have previous+1 port number")
                    .dest("starting-port");

            return parser;
        }

        void validate() {
            checkArgument(deviceCount > 0, "Device count has to be > 0");
            checkArgument(startingPort > 1024, "Starting port has to be > 1024");

            checkArgument(schemasDir.exists(), "Schemas dir has to exist");
            checkArgument(schemasDir.isDirectory(), "Schemas dir has to be a directory");
            checkArgument(schemasDir.canRead(), "Schemas dir has to be readable");
        }
    }

    public static void main(final String[] args) {
        final Params params = parseArgs(args, Params.getParser());
        params.validate();

        final NetconfDeviceSimulator netconfDeviceSimulator = new NetconfDeviceSimulator();
        try {
            netconfDeviceSimulator.start(params);
        } catch (final Exception e) {
            LOG.error("Unhandled exception", e);
            netconfDeviceSimulator.close();
            System.exit(1);
        }

        // Block main thread
        synchronized (netconfDeviceSimulator) {
            try {
                netconfDeviceSimulator.wait();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Params parseArgs(final String[] args, final ArgumentParser parser) {
        final Params opt = new Params();
        try {
            parser.parseArgs(args, opt);
            return opt;
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
        }

        System.exit(1);
        return null;
    }
}
