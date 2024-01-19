/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of methods managing client identifier state on local filesystem.
 */
final class ClientStateUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ClientStateUtils.class);
    private static final String GENERATION_OVERRIDE_PROP_BASE =
        "org.opendaylight.controller.cluster.access.client.initial.generation.";
    private static final String TEST_DIR = "target/test-states";
    private static final String STATE_DIR = "state";
    private static final String FILENAME_FORMAT = "org.opendaylight.controller.cluster.access.client-%s-%s.properties";
    private static final String MEMBER_NAME = "member-name";
    private static final String CLIENT_TYPE = "client-type";
    private static final String GENERATION = "generation";

    private ClientStateUtils() {
        // utility class
    }

    /**
     * Extracts client state from local file, increments generation value, persists the state.
     *
     * <p> If file does not exist new client identifier is generated with initial generation value (see below)
     * and persisted into file for subsequent use.
     *
     * <ul>
     *     <li>The filename is composed using frontendId formatted {@value FILENAME_FORMAT}.</li>
     *     <li>States dir allocation depends on environment: if test environment is detected then it's
     *     {@value TEST_DIR}, otherwise states dir is {@value STATE_DIR}</li>
     *     <li>Initial generation value is taken from system property {@value GENERATION_OVERRIDE_PROP_BASE}
     *     {@code + clientType} (taken from frontendId), if not defined then default value {@code 0} is used</li>
     * </ul>
     *
     * @param frontendId frontend identifier
     * @return client identifier
     */
    static @NonNull ClientIdentifier currentClientIdentifier(final @NonNull FrontendIdentifier frontendId) {
        final var stateFile = stateFile(frontendId);
        final var loadedClientId = loadClientIdentifier(stateFile, frontendId);
        final var currentClientId = loadedClientId == null
            ? ClientIdentifier.create(frontendId, initialGeneration(frontendId.getClientType()))
            : ClientIdentifier.create(frontendId, loadedClientId.getGeneration() + 1);
        saveClientIdentifier(currentClientId);
        return currentClientId;
    }

    @VisibleForTesting
    static @Nullable ClientIdentifier loadClientIdentifier(final @Nullable File stateFile,
            final @NonNull FrontendIdentifier frontendId) {
        if (stateFile.exists()) {
            try (var in = new FileInputStream(stateFile)) {
                final var props = new Properties();
                props.load(in);
                final var readFrontendId = FrontendIdentifier.create(
                    MemberName.forName(props.getProperty(MEMBER_NAME)),
                    FrontendType.forName(props.getProperty(CLIENT_TYPE)));
                final var generation = Long.parseLong(props.getProperty(GENERATION));
                // consistency check
                if (frontendId.equals(readFrontendId)) {
                    return ClientIdentifier.create(frontendId, generation);
                } else {
                    LOG.warn("Client identifier file {} is inconsistent -- expected: {} actual: {}",
                        stateFile, frontendId, readFrontendId);
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.warn("Exception loading client identifier from file {} -> content ignored", stateFile, e);
            }
        }
        return null;
    }

    /**
     * Persists atomically client identifier to file system.
     *
     * <ul>
     *     <li>The filename is composed using frontendId formatted {@value FILENAME_FORMAT}.</li>
     *     <li>States dir allocation depends on environment: if maven build environment is detected then it's
     *     {@value TEST_DIR}, otherwise states dir is {@value STATE_DIR}</li>
     * </ul>
     *
     * @param clientId client identifier
     */
    static void saveClientIdentifier(final @NonNull ClientIdentifier clientId) {
        final var stateFile = stateFile(clientId.getFrontendId());
        final File tempFile;
        try {
            tempFile = File.createTempFile("client-identifier", ".temp");
        } catch (IOException e) {
            LOG.error("Cannot create temp file", e);
            return;
        }
        try (var out = new FileOutputStream(tempFile)) {
            final var props = new Properties();
            props.setProperty(MEMBER_NAME, clientId.getFrontendId().getMemberName().getName());
            props.setProperty(CLIENT_TYPE, clientId.getFrontendId().getClientType().getName());
            props.setProperty(GENERATION, String.valueOf(clientId.getGeneration()));
            props.store(out, "auto-updated, don't remove");
        } catch (IOException e) {
            LOG.error("Cannot create temp file", e);
            return;
        }
        try {
            if (stateFile.getParentFile().exists() || stateFile.getParentFile().mkdirs()) {
                Files.move(tempFile.toPath(), stateFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } else {
                throw new IOException("Could not create parent dirs for " + stateFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error updating clientId file " + stateFile.getAbsoluteFile(), e);
        }
    }

    @VisibleForTesting
    static File stateFile(final @NonNull FrontendIdentifier frontendId) {
        final var parentDir = isBuildEnv() ? new File(TEST_DIR) : new File(STATE_DIR);
        final var filename = FILENAME_FORMAT.formatted(frontendId.getMemberName().getName(),
            frontendId.getClientType().getName());
        return new File(parentDir, filename);
    }

    private static long initialGeneration(final FrontendType clientType) {
        final String propName = GENERATION_OVERRIDE_PROP_BASE + clientType.getName();
        final String propValue = System.getProperty(propName);
        if (propValue == null) {
            return 0;
        }
        try {
            return Long.parseUnsignedLong(propValue);
        } catch (NumberFormatException e) {
            LOG.warn("Exception parsing initial generation value from {} = {}, using 0", propName, propValue, e);
            return 0;
        }
    }

    private static boolean isBuildEnv() {
        return new File("target").exists();
    }
}
