/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MAX_ENTRY_SIZE;
import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MAX_ENTRY_SIZE_DEFAULT;
import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MAX_SEGMENT_SIZE;
import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MAX_SEGMENT_SIZE_DEFAULT;
import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MAX_UNFLUSHED_BYTES;
import static org.opendaylight.controller.akka.segjournal.SegmentedFileJournal.STORAGE_MEMORY_MAPPED;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.atomix.storage.journal.StorageLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

@SuppressWarnings("RegexpSinglelineJava")
final class BenchmarkUtils {

    static final String PROG_NAME = "segjourlan-benchmark";

    static final String BENCHMARK_USE_CURRENT = "current";
    static final String BENCHMARK_NUMBER_OF_MESSAGES = "messages-num";
    static final String BENCHMARK_PAYLOAD_SIZE = "payload-size";
    static final String BENCHMARK_PAYLOAD_SIZE_DEFAULT = "10K";

    static final String CURRENT_CONFIG_RESOURCE = "/initial/factory-pekko.conf";
    static final String CURRENT_CONFIG_PATH = "odl-cluster-data.akka.persistence.journal.segmented-file";

    private static final String[] BYTE_SFX = {"G", "M", "K"};
    private static final int[] BYTE_THRESH = {1024 * 1024 * 1024, 1024 * 1024, 1024};

    record BenchmarkConfig(StorageLevel storage, Path workingDir, int maxEntrySize, int maxSegmentSize,
        int maxUnflushedBytes, int payloadSize, int messagesNum) {
    }

    private BenchmarkUtils() {
        // utility class
    }

    static BenchmarkConfig buildConfig(final String[] args) {
        final var parser = getArgumentParser();
        final var paramsMap = new HashMap<String, Object>();
        try {
            parser.parseArgs(args, paramsMap);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return null;
        }
        return toConfig(paramsMap);
    }

    private static ArgumentParser getArgumentParser() {
        final var parser = ArgumentParsers.newArgumentParser(PROG_NAME).defaultHelp(true);

        parser.description("Performs asynchronous write to segmented journal, collects and prints variety of metrics");

        parser.addArgument("--current")
            .type(Boolean.class).setDefault(Boolean.FALSE)
            .action(Arguments.storeConst()).setConst(Boolean.TRUE)
            .dest(BENCHMARK_USE_CURRENT)
            .help("indicates base configuration to be taken from current cluster configuration, "
                + "all other arguments excepting 'requests' and 'payload size' will be ignored");

        parser.addArgument("--memory-mapped")
            .type(Boolean.class).setDefault(Boolean.FALSE)
            .action(Arguments.storeConst()).setConst(Boolean.TRUE)
            .dest(STORAGE_MEMORY_MAPPED)
            .help("indicates mapping journal segments to memory, otherwise file system is used");

        parser.addArgument("-e", "--max-entry-size")
            .type(String.class).setDefault(formatBytes(STORAGE_MAX_ENTRY_SIZE_DEFAULT))
            .dest(STORAGE_MAX_ENTRY_SIZE)
            .help("max entry size, bytes format");

        parser.addArgument("-s", "--max-segment-size")
            .type(String.class).setDefault(formatBytes(STORAGE_MAX_SEGMENT_SIZE_DEFAULT))
            .dest(STORAGE_MAX_SEGMENT_SIZE)
            .help("max segment size, bytes  ");

        parser.addArgument("-u", "--max-unflushed-bytes")
            .type(String.class)
            .dest(STORAGE_MAX_UNFLUSHED_BYTES)
            .help("max unflushed bytes, bytes format, "
                + "if not defined the value is taken from 'max-entry-size'");

        parser.addArgument("-n", "--messages-num")
            .type(Integer.class).required(true)
            .dest(BENCHMARK_NUMBER_OF_MESSAGES)
            .setDefault(10_000)
            .help("number of messages to write");

        parser.addArgument("-p", "--payload-size")
            .type(String.class).setDefault(BENCHMARK_PAYLOAD_SIZE_DEFAULT)
            .dest(BENCHMARK_PAYLOAD_SIZE)
            .help("median for request payload size, bytes format supported, "
                + "actual size is variable 80% to 120% from defined median value");

        return parser;
    }

    static BenchmarkConfig toConfig(final Map<String, Object> paramsMap) {
        final var inputConfig = ConfigFactory.parseMap(paramsMap);
        final var finalConfig = (Boolean) paramsMap.get(BENCHMARK_USE_CURRENT)
            ? currentConfig().withFallback(inputConfig) : inputConfig;

        final var benchmarkConfig = new BenchmarkConfig(
            finalConfig.getBoolean(STORAGE_MEMORY_MAPPED) ? StorageLevel.MAPPED : StorageLevel.DISK,
            createTempDirectory(),
            bytes(finalConfig, STORAGE_MAX_ENTRY_SIZE),
            bytes(finalConfig, STORAGE_MAX_SEGMENT_SIZE),
            finalConfig.hasPath(STORAGE_MAX_UNFLUSHED_BYTES)
                ? bytes(finalConfig, STORAGE_MAX_UNFLUSHED_BYTES) : bytes(finalConfig, STORAGE_MAX_ENTRY_SIZE),
            bytes(finalConfig, BENCHMARK_PAYLOAD_SIZE),
            finalConfig.getInt(BENCHMARK_NUMBER_OF_MESSAGES)
        );
        // validate
        if (benchmarkConfig.payloadSize > benchmarkConfig.maxEntrySize) {
            printAndExit("payloadSize should be less than maxEntrySize");
        }
        return benchmarkConfig;
    }

    private static int bytes(final Config config, final String key) {
        final var bytesLong = config.getBytes(key);
        if (bytesLong <= 0 || bytesLong > Integer.MAX_VALUE) {
            printAndExit(
                key + " value (" + bytesLong + ") is invalid, expected in range 1 .. " + Integer.MAX_VALUE);
        }
        return bytesLong.intValue();
    }

    static Config currentConfig() {
        try (var in = BenchmarkUtils.class.getResourceAsStream(CURRENT_CONFIG_RESOURCE)) {
            final var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final var globalConfig = ConfigFactory.parseString(content);
            final var currentConfig = globalConfig.getConfig(CURRENT_CONFIG_PATH);
            System.out.println("Current configuration loaded from " + CURRENT_CONFIG_RESOURCE);
            return currentConfig;

        } catch (IOException e) {
            printAndExit("Error loading current configuration from resource " + CURRENT_CONFIG_RESOURCE, e);
            return null;
        }
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory(PROG_NAME);
        } catch (IOException e) {
            printAndExit("Cannot create temp directory", e);
        }
        return null;
    }

    private static void printAndExit(final String message) {
        printAndExit(message, null);
    }

    private static void printAndExit(final String message, final Exception exception) {
        System.err.println(message);
        if (exception != null) {
            exception.printStackTrace(System.err);
        }
        System.exit(1);
    }

    static String formatBytes(final int bytes) {
        for (int i = 0; i < 3; i++) {
            if (bytes > BYTE_THRESH[i]) {
                return bytes / BYTE_THRESH[i] + BYTE_SFX[i];
            }
        }
        return String.valueOf(bytes);
    }

    static String formatNanos(final double nanos) {
        return formatNanos(Math.round(nanos));
    }

    static String formatNanos(final long nanos) {
        return Stopwatch.createStarted(new Ticker() {
            boolean started;

            @Override
            public long read() {
                if (started) {
                    return nanos;
                }
                started = true;
                return 0;
            }
        }).toString();
    }

    static String toMetricId(final String metricKey) {
        return metricKey.substring(metricKey.lastIndexOf('.') + 1);
    }
}
