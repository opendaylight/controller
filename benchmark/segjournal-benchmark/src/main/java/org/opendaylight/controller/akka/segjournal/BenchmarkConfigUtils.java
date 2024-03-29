/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.commons.io.FileUtils;

final class BenchmarkConfigUtils {

    static final String PROG_NAME = "segjourlan-benchmark";

    static final String BENCHMARK_USE_CURRENT = "current";
    static final String BENCHMARK_REQUESTS_NUMBER = "requests";
    static final String BENCHMARK_PAYLOAD_SIZE = "payload-size";
    static final String BENCHMARK_PAYLOAD_SIZE_DEFAULT = "10K";

    static final String CURRENT_CONFIG_RESOURCE = "/initial/factory-akka.conf";
    static final String CURRENT_CONFIG_PATH = "odl-cluster-data.akka.persistence.journal.segmented-file";

    record BenchmarkConfig(StorageLevel storage, File workingDir, int maxEntrySize, int maxSegmentSize,
        int maxUnflushedBytes, int payloadSize, int requestsNum) {
    }

    private BenchmarkConfigUtils() {
        // utility class
    }

    static BenchmarkConfig buildConfig(final String[] args) {
        final var parser = getParser();
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

    private static ArgumentParser getParser() {
        final var parser = ArgumentParsers.newArgumentParser(PROG_NAME).defaultHelp(true);

        parser.description("Performs asynchronous write to segmented journal, collects and prints variety of metrics");

        parser.addArgument("--current")
            .type(Boolean.class).setDefault(Boolean.FALSE)
            .action(Arguments.storeConst()).setConst(Boolean.TRUE)
            .dest(BENCHMARK_USE_CURRENT)
            .help("indicates base configuration to be taken from current cluster configuration, " +
                "all other arguments excepting 'requests' and 'payload size' will be ignored");

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
            .help("max unflushed bytes, bytes format, " +
                "if not defined the value is taken from 'max-entry-size'");

        parser.addArgument("-n", "--requests")
            .type(Integer.class).required(true)
            .dest(BENCHMARK_REQUESTS_NUMBER)
            .setDefault(10_000)
            .help("number of requests to perform");

        parser.addArgument("-p", "--payload-size")
            .type(Integer.class).setDefault(BENCHMARK_PAYLOAD_SIZE_DEFAULT)
            .dest(BENCHMARK_PAYLOAD_SIZE)
            .help("median for request payload size, bytes format supported, " +
                "actual size is variable 80% to 120% from defined median value");

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
            finalConfig.getInt(BENCHMARK_REQUESTS_NUMBER)
        );
        return validate(benchmarkConfig);
    }

    private static int bytes(final Config config, final String key) {
        final var bytesLong = config.getBytes(key);
        if (bytesLong <= 0 || bytesLong > Integer.MAX_VALUE) {
            System.err.println(
                key + " value (" + bytesLong + ") is invalid, expected in range 1 .. " + Integer.MAX_VALUE);
            System.exit(1);
        }
        return bytesLong.intValue();
    }

    private static BenchmarkConfig validate(final BenchmarkConfig config) {
        // TODO
        return config;
    }

    static Config currentConfig() {
        try (var in = BenchmarkConfigUtils.class.getResourceAsStream(CURRENT_CONFIG_RESOURCE)) {
            final var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final var globalConfig = ConfigFactory.parseString(content);
            final var currentConfig = globalConfig.getConfig(CURRENT_CONFIG_PATH);
            System.out.println("Current configuration loaded from " + CURRENT_CONFIG_RESOURCE);
            return currentConfig;

        } catch (IOException e) {
            System.err.println("Error loading current configuration from resource " + CURRENT_CONFIG_RESOURCE);
            e.printStackTrace(System.err);
            System.exit(1);
            return null;
        }
    }

    private static File createTempDirectory() {
        try {
            return Files.createTempDirectory(PROG_NAME).toFile();
        } catch (IOException e) {
            System.err.println("Cannot create temp directory");
            e.printStackTrace(System.err);
        }
        System.exit(1);
        return null;
    }

    static String formatBytes(int bytes) {
        final var formatted = FileUtils.byteCountToDisplaySize(bytes);
        return formatted.replaceAll("[\\s|B|\\sbytes]", "");
    }
}
