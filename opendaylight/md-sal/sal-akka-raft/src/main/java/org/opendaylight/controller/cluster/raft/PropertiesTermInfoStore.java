/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TermInfoStore} based on an atomic {@link Properties} file.
 */
@NonNullByDefault
final class PropertiesTermInfoStore implements TermInfoStore {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesTermInfoStore.class);
    private static final String PROP_TERM = "term";
    private static final String PROP_VOTED_FOR = "votedFor";

    private final String logId;
    private final Path propFile;

    private TermInfo currentTerm = TermInfo.INITIAL;

    PropertiesTermInfoStore(final String logId, final Path propFile) {
        this.logId = requireNonNull(logId);
        this.propFile = requireNonNull(propFile);
    }

    @Override
    public TermInfo currentTerm() {
        return currentTerm;
    }

    @Override
    public void setTerm(final TermInfo newTerm) {
        doSetTerm(requireNonNull(newTerm));
    }

    private void doSetTerm(final TermInfo newTerm) {
        currentTerm = requireNonNull(newTerm);
        LOG.debug("{}: Set currentTerm={}, votedFor={}", logId, newTerm.term(), newTerm.votedFor());
    }

    @Override
    public void storeAndSetTerm(final TermInfo newTerm) throws IOException {
        final var props = new Properties(2);
        props.setProperty(PROP_TERM, Long.toString(newTerm.term()));
        final var votedFor = newTerm.votedFor();
        if (votedFor != null) {
            props.setProperty(PROP_VOTED_FOR, votedFor);
        }

        saveFile(props);
        doSetTerm(newTerm);
    }

    @Override
    public @Nullable TermInfo loadAndSetTerm() throws IOException {
        final Properties props;
        try {
            props = loadFile();
        } catch (NoSuchFileException e) {
            LOG.debug("{} does not exist", propFile, e);
            return null;
        }

        final var termProp = props.getProperty(PROP_TERM);
        if (termProp == null) {
            throw new IOException("Missing " + PROP_TERM + " in " + props);
        }

        final long term;
        try {
            term = Long.parseLong(termProp);
        } catch (NumberFormatException e) {
            throw new IOException("Malformed term " + termProp, e);
        }

        final var newTerm = new TermInfo(term, props.getProperty(PROP_VOTED_FOR));
        doSetTerm(newTerm);
        return newTerm;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("file", propFile).toString();
    }

    private Properties loadFile() throws IOException {
        final var props = new Properties();
        try (var is = Files.newInputStream(propFile)) {
            props.load(is);
        }
        return props;
    }

    private void saveFile(final Properties props) throws IOException {
        final var parent = propFile.getParent();
        Files.createDirectories(parent);

        final var tmp = Files.createTempFile(parent, propFile.getFileName().toString(), null);
        try {
            try (var os = Files.newOutputStream(tmp,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC)) {
                props.store(os, "RAFT voting state. Do not touch unless you know what you are doing!");
            }

            Files.move(tmp, propFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e) {
                LOG.warn("Failed to delete {}", tmp, e);
            }
        }
    }
}
