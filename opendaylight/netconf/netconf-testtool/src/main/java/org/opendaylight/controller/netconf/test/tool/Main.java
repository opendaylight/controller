/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public final class Main {

    // TODO add logback config

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    static class Params {

        @Arg(dest = "schemas-dir")
        public File schemasDir;

        @Arg(dest = "devices-count")
        public int deviceCount;

        @Arg(dest = "starting-port")
        public int startingPort;

        @Arg(dest = "generate-configs-dir")
        public File generateConfigsDir;

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
                    .setDefault(17830)
                    .help("First port for simulated device. Each other device will have previous+1 port number")
                    .dest("starting-port");

            parser.addArgument("--generate-configs-dir")
                    .type(File.class)
                    .help("Directory where initial config files for ODL distribution should be generated")
                    .dest("generate-configs-dir");

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
            final List<Integer> openDevices = netconfDeviceSimulator.start(params);
            if(params.generateConfigsDir != null) {
                new ConfigGenerator(params.generateConfigsDir, openDevices).invoke();
            }
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

    private static class ConfigGenerator {
        public static final String NETCONF_CONNECTOR_XML = "/initial/99-netconf-connector.xml";
        public static final String NETCONF_CONNECTOR_NAME = "controller-config";
        public static final String NETCONF_CONNECTOR_PORT = "1830";
        public static final String SIM_DEVICE_SUFFIX = "-sim-device";

        private final File directory;
        private final List<Integer> openDevices;

        public ConfigGenerator(final File directory, final List<Integer> openDevices) {
            this.directory = directory;
            this.openDevices = openDevices;
        }

        public void invoke() {
            if(directory.exists() == false) {
                checkState(directory.mkdirs(), "Unable to create folder %s" + directory);
            }

            try(InputStream stream = Main.class.getResourceAsStream(NETCONF_CONNECTOR_XML)) {
                checkNotNull(stream, "Cannot load %s", NETCONF_CONNECTOR_XML);
                String configBlueprint = CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));

                // TODO make address configurable
                checkState(configBlueprint.contains(NETCONF_CONNECTOR_NAME));
                checkState(configBlueprint.contains(NETCONF_CONNECTOR_PORT));
                configBlueprint = configBlueprint.replace(NETCONF_CONNECTOR_NAME, "%s");
                configBlueprint = configBlueprint.replace(NETCONF_CONNECTOR_PORT, "%s");

                for (final Integer openDevice : openDevices) {
                    final String name = String.valueOf(openDevice) + SIM_DEVICE_SUFFIX;
                    final String configContent = String.format(configBlueprint, name, String.valueOf(openDevice));
                    Files.write(configContent, new File(directory, name + ".xml"), Charsets.UTF_8);
                }

                LOG.info("Config files generated in {}", directory);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to generate config files", e);
            }
        }
    }
}
