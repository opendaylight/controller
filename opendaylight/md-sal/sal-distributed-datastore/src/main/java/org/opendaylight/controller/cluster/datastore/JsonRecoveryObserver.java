/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.gson.stream.JsonWriter;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.raft.spi.RecoveryObserver;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RecoveryObserver} dumping data into files.
 */
final class JsonRecoveryObserver implements RecoveryObserver {
    private static final class NodeIterator {
        private final Iterator<DataTreeCandidateNode> iterator;
        private final YangInstanceIdentifier path;
        private final NodeIterator parent;

        NodeIterator(final @Nullable NodeIterator parent, final YangInstanceIdentifier path,
                     final Iterator<DataTreeCandidateNode> iterator) {
            this.iterator = requireNonNull(iterator);
            this.path = requireNonNull(path);
            this.parent = parent;
        }

        NodeIterator next(final JsonWriter writer) throws IOException {
            while (iterator.hasNext()) {
                final var node = iterator.next();
                final var child = path.node(node.name());

                switch (node.modificationType()) {
                    case null -> throw new NullPointerException();
                    case APPEARED, DISAPPEARED, SUBTREE_MODIFIED -> {
                        return new NodeIterator(this, child, node.childNodes().iterator());
                    }
                    case DELETE, UNMODIFIED, WRITE -> outputNodeInfo(writer, path, node);
                }
            }

            return parent;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsonRecoveryObserver.class);

    private final String memberId;
    private final Path dirPath;
    private final ShardDataTree store;

    private JsonWriter writer;

    JsonRecoveryObserver(final String memberId, final Path dirPath, final ShardDataTree store) {
        this.memberId = requireNonNull(memberId);
        this.dirPath = requireNonNull(dirPath);
        this.store = requireNonNull(store);
    }

    @Override
    public void onSnapshotRecovered(final StateSnapshot snapshot) {
        final var root = store.readNode(YangInstanceIdentifier.of()).orElse(null);
        if (!(root instanceof NormalizedNodeContainer<?> container)) {
            throw new IllegalStateException("Unexpected root " + root);
        }

        final var snapshotDir = dirPath.resolve("snapshots");
        createDir(snapshotDir);

        final var filePath = snapshotDir.resolve(memberId + "-snapshot.json");
        LOG.debug("Creating JSON file : {}", filePath);

        try (var jsonWriter = new JsonWriter(Files.newBufferedWriter(filePath))) {
            jsonWriter.beginObject();

            try (var nnWriter = NormalizedNodeWriter.forStreamWriter(JSONNormalizedNodeStreamWriter.createNestedWriter(
                    JSONCodecFactorySupplier.RFC7951.getShared(store.modelContext()), jsonWriter, null), true)) {
                for (var node : container.body()) {
                    nnWriter.write(node);
                }
            }

            jsonWriter.endObject();
        } catch (IOException e) {
            LOG.error("Failed to export stapshot to {}", filePath, e);
            return;
        }

        LOG.debug("Created JSON file: {}", filePath);
    }

    @Override
    public void onCommandRecovered(final StateMachineCommand command) {
        final var local = ensureWriter();
        try {
            if (command instanceof CommitTransactionPayload payload) {
                writeNode(local, payload.getCandidate().candidate());
            } else {
                local.beginObject().name("Payload").value(command.toString()).endObject();
            }
        } catch (IOException e) {
            LOG.warn("{}: failed to export journal", memberId, e);
        }
    }

    @Override
    public void onRecoveryCompleted() {
        final var local = writer;
        writer = null;
        if (local != null) {
            try {
                local.endArray().endObject().close();
            } catch (IOException e) {
                LOG.warn("{}: failed to close writer", memberId, e);
            }
        }
    }

    private @NonNull JsonWriter ensureWriter() {
        var local = writer;
        if (local == null) {
            final var journalDir = dirPath.resolve("journals");
            createDir(journalDir);

            final var filePath = journalDir.resolve(memberId + "-journal.json");
            LOG.debug("Creating JSON file : {}", filePath);

            final BufferedWriter open;
            try {
                open = Files.newBufferedWriter(filePath);
            } catch (IOException e) {
                LOG.error("Failed to export journal to {}", filePath, e);
                throw new UncheckedIOException(e);
            }

            local = new JsonWriter(open);
            try {
                local.beginObject().name("Entries").beginArray();
            } catch (IOException e) {
                LOG.error("Failed to export journal to {}", filePath, e);
                try {
                    local.close();
                } catch (IOException e1) {
                    e.addSuppressed(e1);
                }
                throw new UncheckedIOException(e);
            }

            writer = local;
        }
        return local;
    }

    private static void createDir(final Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create directory", e);
        }
    }

    private static void writeNode(final JsonWriter writer, final DataTreeCandidate candidate) throws IOException {
        writer.beginObject().name("Entry").beginArray();
        doWriteNode(writer, candidate.getRootPath(), candidate.getRootNode());
        writer.endArray().endObject();
    }

    private static void doWriteNode(final JsonWriter writer, final YangInstanceIdentifier path,
            final DataTreeCandidateNode node) throws IOException {
        switch (node.modificationType()) {
            case null -> throw new NullPointerException();
            case APPEARED, DISAPPEARED, SUBTREE_MODIFIED -> {
                var iterator = new NodeIterator(null, path, node.childNodes().iterator());
                do {
                    iterator = iterator.next(writer);
                } while (iterator != null);
            }
            case DELETE, UNMODIFIED, WRITE -> outputNodeInfo(writer, path, node);
        }
    }

    private static void outputNodeInfo(final JsonWriter writer, final YangInstanceIdentifier path,
            final DataTreeCandidateNode node) throws IOException {
        final var modificationType = node.modificationType();

        writer.beginObject().name("Node");
        writer.beginArray();
        writer.beginObject().name("Path").value(path.toString()).endObject();
        writer.beginObject().name("ModificationType").value(modificationType.toString()).endObject();
        if (modificationType == ModificationType.WRITE) {
            writer.beginObject().name("Data").value(node.getDataAfter().body().toString()).endObject();
        }
        writer.endArray();
        writer.endObject();
    }
}
