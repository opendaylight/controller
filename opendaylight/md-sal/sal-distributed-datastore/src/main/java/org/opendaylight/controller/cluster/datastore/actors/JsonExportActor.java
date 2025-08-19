/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static java.util.Objects.requireNonNull;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public final class JsonExportActor extends AbstractUntypedActor {
    // Internal messages
    public static final class ExportSnapshot {
        private final String id;

        private final DataTreeCandidate dataTreeCandidate;

        public ExportSnapshot(final DataTreeCandidate candidate, final String id) {
            dataTreeCandidate = requireNonNull(candidate);
            this.id = requireNonNull(id);
        }
    }

    public static final class ExportJournal {
        private final LogEntry entry;

        public ExportJournal(final LogEntry entry) {
            this.entry = requireNonNull(entry);
        }
    }

    public static final class FinishExport {
        private final String id;

        public FinishExport(final String id) {
            this.id = requireNonNull(id);
        }
    }

    private final List<LogEntry> entries = new ArrayList<>();
    private final @NonNull EffectiveModelContext modelContext;
    private final @NonNull Path baseDirPath;

    private JsonExportActor(final EffectiveModelContext modelContext, final Path dirPath) {
        this.modelContext = requireNonNull(modelContext);
        baseDirPath = requireNonNull(dirPath);
    }

    public static Props props(final EffectiveModelContext schemaContext, final String dirPath) {
        return Props.create(JsonExportActor.class, schemaContext, Path.of(dirPath));
    }

    @Override
    protected void handleReceive(final Object message) {
        switch (message) {
            case ExportJournal msg -> onExportJournal(msg);
            case ExportSnapshot msg -> onExportSnapshot(msg);
            case FinishExport msg -> onFinishExport(msg);
            default -> unknownMessage(message);
        }
    }

    private void onExportSnapshot(final ExportSnapshot exportSnapshot) {
        final Path snapshotDir = baseDirPath.resolve("snapshots");
        createDir(snapshotDir);

        final Path filePath = snapshotDir.resolve(exportSnapshot.id + "-snapshot.json");
        LOG.debug("Creating JSON file : {}", filePath);

        final var root = exportSnapshot.dataTreeCandidate.getRootNode().getDataAfter();
        if (root instanceof NormalizedNodeContainer<?> container) {
            writeSnapshot(filePath, container);
            LOG.debug("Created JSON file: {}", filePath);
        } else {
            throw new IllegalStateException("Unexpected root " + root);
        }
    }

    private void onExportJournal(final ExportJournal exportJournal) {
        entries.add(exportJournal.entry);
    }

    private void onFinishExport(final FinishExport finishExport) {
        final var journalDir = baseDirPath.resolve("journals");
        createDir(journalDir);

        final Path filePath = journalDir.resolve(finishExport.id + "-journal.json");
        LOG.debug("Creating JSON file : {}", filePath);
        writeJournal(filePath);
        LOG.debug("Created JSON file: {}", filePath);
    }

    private void writeSnapshot(final Path path, final NormalizedNodeContainer<?> root) {
        try (var jsonWriter = new JsonWriter(Files.newBufferedWriter(path))) {
            jsonWriter.beginObject();

            try (var nnWriter = NormalizedNodeWriter.forStreamWriter(JSONNormalizedNodeStreamWriter.createNestedWriter(
                    JSONCodecFactorySupplier.RFC7951.getShared(modelContext), jsonWriter, null), true)) {
                for (NormalizedNode node : root.body()) {
                    nnWriter.write(node);
                }
            }

            jsonWriter.endObject();
        } catch (IOException e) {
            LOG.error("Failed to export stapshot to {}", path, e);
        }
    }

    private void writeJournal(final Path path) {
        try (JsonWriter jsonWriter = new JsonWriter(Files.newBufferedWriter(path))) {
            jsonWriter.beginObject().name("Entries");
            jsonWriter.beginArray();
            for (var entry : entries) {
                final var data = entry.command();
                if (data instanceof CommitTransactionPayload payload) {
                    final var candidate = payload.getCandidate().candidate();
                    writeNode(jsonWriter, candidate);
                } else {
                    jsonWriter.beginObject().name("Payload").value(data.toString()).endObject();
                }
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        } catch (IOException e) {
            LOG.error("Failed to export journal to {}", path, e);
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
                NodeIterator iterator = new NodeIterator(null, path, node.childNodes().iterator());
                do {
                    iterator = iterator.next(writer);
                } while (iterator != null);
            }
            case DELETE, UNMODIFIED, WRITE -> outputNodeInfo(writer, path, node);
        }
    }

    private static void outputNodeInfo(final JsonWriter writer, final YangInstanceIdentifier path,
                                       final DataTreeCandidateNode node) throws IOException {
        final ModificationType modificationType = node.modificationType();

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

    private void createDir(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOG.warn("Directory {} cannot be created", path, e);
        }
    }

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
}
