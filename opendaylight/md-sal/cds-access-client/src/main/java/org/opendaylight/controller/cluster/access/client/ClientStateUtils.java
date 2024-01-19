/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClientStateUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ClientStateUtils.class);
    /*
     * Base for the property name which overrides the initial generation when we fail to find anything from persistence.
     * The actual property name has the frontend type name appended.
     */
    private static final String GENERATION_OVERRIDE_PROP_BASE =
        "org.opendaylight.controller.cluster.access.client.initial.generation.";
    private static final String STATE_DIR_PROP = "org.opendaylight.controller.cluster.access.client.state.dir";
    private static final String STATE_DIR_DEFAULT = "state";
    private static final String FILENAME_FORMAT = "org.opendaylight.controller.cluster.access.client-%s-%s.properties";
    private static final String MEMBER_NAME = "member-name";
    private static final String CLIENT_TYPE = "client-type";
    private static final String GENERATION = "generation";

    private ClientStateUtils() {
        // utility class
    }

    static @NonNull ClientIdentifier loadClientIdentifier(final @NonNull FrontendIdentifier frontendId) {
        final var stateFile = stateFile(frontendId);
        ClientIdentifier clientId = null;
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
                    // increment generation value
                    clientId = ClientIdentifier.create(readFrontendId, generation + 1);
                } else {
                    LOG.warn("Client identifier file {} is inconsistent -- expected: {} actual: {}",
                        stateFile, frontendId, readFrontendId);
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.warn("Exception loading client identifier from file {} -> content ignored", stateFile, e);
            }
        }
        if (clientId == null) {
            clientId = ClientIdentifier.create(frontendId, initialGeneration(frontendId.getClientType()));
        }
        // persist initial or update generation
        saveClientIdentifier(clientId);
        return clientId;
    }

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

    private static File stateFile(final @NonNull FrontendIdentifier frontendId) {
        final var parentDir = new File(System.getProperty(STATE_DIR_PROP, STATE_DIR_DEFAULT));
        final var filename = FILENAME_FORMAT.formatted(frontendId.getMemberName().getName(),
            frontendId.getClientType().getName());
        return new File(parentDir, filename);
    }

    static long initialGeneration(final FrontendType clientType) {
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
}
