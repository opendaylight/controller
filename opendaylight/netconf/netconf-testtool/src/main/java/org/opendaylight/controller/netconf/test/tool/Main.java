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

import java.util.concurrent.TimeUnit;
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

    // TODO make exi configurable

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    static class Params {

        @Arg(dest = "schemas-dir")
        public File schemasDir;

        @Arg(dest = "devices-count")
        public int deviceCount;

        @Arg(dest = "starting-port")
        public int startingPort;

        @Arg(dest = "generate-config-connection-timeout")
        public int generateConfigsTimeout;

        @Arg(dest = "generate-configs-dir")
        public File generateConfigsDir;

        @Arg(dest = "generate-configs-batch-size")
        public int generateConfigBatchSize;

        @Arg(dest = "ssh")
        public boolean ssh;

        @Arg(dest = "exi")
        public boolean exi;

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

            parser.addArgument("--generate-config-connection-timeout")
                    .type(Integer.class)
                    .setDefault((int)TimeUnit.MINUTES.toMillis(5))
                    .help("Timeout to be generated in initial config files")
                    .dest("generate-config-connection-timeout");

            parser.addArgument("--generate-configs-batch-size")
                    .type(Integer.class)
                    .setDefault(100)
                    .help("Number of connector configs per generated file")
                    .dest("generate-configs-batch-size");

            parser.addArgument("--generate-configs-dir")
                    .type(File.class)
                    .help("Directory where initial config files for ODL distribution should be generated")
                    .dest("generate-configs-dir");

            parser.addArgument("--ssh")
                    .type(Boolean.class)
                    .setDefault(true)
                    .help("Whether to use ssh for transport or just pure tcp")
                    .dest("ssh");

            parser.addArgument("--exi")
                    .type(Boolean.class)
                    .setDefault(false)
                    .help("Whether to use exi to transport xml content")
                    .dest("exi");

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
        ch.ethz.ssh2.log.Logger.enabled = true;

        final Params params = parseArgs(args, Params.getParser());
        params.validate();

        final NetconfDeviceSimulator netconfDeviceSimulator = new NetconfDeviceSimulator();
        try {
            final List<Integer> openDevices = netconfDeviceSimulator.start(params);
            if(params.generateConfigsDir != null) {
                new ConfigGenerator(params.generateConfigsDir, openDevices).generate(params.ssh, params.generateConfigBatchSize, params.generateConfigsTimeout);
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
        public static final String NETCONF_USE_SSH = "false";
        public static final String SIM_DEVICE_SUFFIX = "-sim-device";

        private final File directory;
        private final List<Integer> openDevices;

        public ConfigGenerator(final File directory, final List<Integer> openDevices) {
            this.directory = directory;
            this.openDevices = openDevices;
        }

        public void generate(final boolean useSsh, final int batchSize, final int generateConfigsTimeout) {
            if(directory.exists() == false) {
                checkState(directory.mkdirs(), "Unable to create folder %s" + directory);
            }

            try(InputStream stream = Main.class.getResourceAsStream(NETCONF_CONNECTOR_XML)) {
                checkNotNull(stream, "Cannot load %s", NETCONF_CONNECTOR_XML);
                String configBlueprint = CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));

                // TODO make address configurable
                checkState(configBlueprint.contains(NETCONF_CONNECTOR_NAME));
                checkState(configBlueprint.contains(NETCONF_CONNECTOR_PORT));
                checkState(configBlueprint.contains(NETCONF_USE_SSH));
                configBlueprint = configBlueprint.replace(NETCONF_CONNECTOR_NAME, "%s");
                configBlueprint = configBlueprint.replace(NETCONF_CONNECTOR_PORT, "%s");
                configBlueprint = configBlueprint.replace(NETCONF_USE_SSH, "%s");

                final String before = configBlueprint.substring(0, configBlueprint.indexOf("<module>"));
                final String middleBlueprint = configBlueprint.substring(configBlueprint.indexOf("<module>"), configBlueprint.indexOf("</module>"));
                final String after = configBlueprint.substring(configBlueprint.indexOf("</module>") + "</module>".length());

                int connectorCount = 0;
                Integer batchStart = null;
                StringBuilder b = new StringBuilder();
                b.append(before);

                for (final Integer openDevice : openDevices) {
                    if(batchStart == null) {
                        batchStart = openDevice;
                    }

                    final String name = String.valueOf(openDevice) + SIM_DEVICE_SUFFIX;
                    String configContent = String.format(middleBlueprint, name, String.valueOf(openDevice), String.valueOf(!useSsh));
                    configContent = String.format("%s%s%d%s\n%s\n", configContent, "<connection-timeout-millis>", generateConfigsTimeout, "</connection-timeout-millis>", "</module>");

                    b.append(configContent);
                    connectorCount++;
                    if(connectorCount == batchSize) {
                        b.append(after);
                        Files.write(b.toString(), new File(directory, String.format("simulated-devices_%d-%d.xml", batchStart, openDevice)), Charsets.UTF_8);
                        connectorCount = 0;
                        b = new StringBuilder();
                        b.append(before);
                        batchStart = null;
                    }
                }

                // Write remaining
                if(connectorCount != 0) {
                    b.append(after);
                    Files.write(b.toString(), new File(directory, String.format("simulated-devices_%d-%d.xml", batchStart, openDevices.get(openDevices.size() - 1))), Charsets.UTF_8);
                }

                LOG.info("Config files generated in {}", directory);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to generate config files", e);
            }
        }
    }
}
