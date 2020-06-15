/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.Props;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class JsonExportActor extends AbstractUntypedActor {
    // Internal messages
    public static final class ExportSnapshot {
        private final String id;

        private final DataTreeCandidate dataTreeCandidate;

        public ExportSnapshot(final DataTreeCandidate candidate, final String id) {
            this.dataTreeCandidate = requireNonNull(candidate);
            this.id = requireNonNull(id);
        }
    }

    public static final class ExportJournal {
        private final ReplicatedLogEntry replicatedLogEntry;

        public ExportJournal(final ReplicatedLogEntry replicatedLogEntry) {
            this.replicatedLogEntry = requireNonNull(replicatedLogEntry);
        }
    }

    public static final class FinishExport {
        private final String id;

        public FinishExport(final String id) {
            this.id = requireNonNull(id);
        }
    }

    private final List<ReplicatedLogEntry> entries = new ArrayList<>();
    private final EffectiveModelContext schemaContext;
    private final Path baseDirPath;

    private JsonExportActor(final EffectiveModelContext schemaContext, final Path dirPath) {
        this.schemaContext = requireNonNull(schemaContext);
        this.baseDirPath = requireNonNull(dirPath);
    }

    public static Props props(final EffectiveModelContext schemaContext, final String dirPath) {
        return Props.create(JsonExportActor.class, schemaContext, Paths.get(dirPath));
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExportSnapshot) {
            onExportSnapshot((ExportSnapshot) message);
        } else if (message instanceof ExportJournal) {
            onExportJournal((ExportJournal) message);
        } else if (message instanceof FinishExport) {
            onFinishExport((FinishExport)message);
        } else {
            unknownMessage(message);
        }
    }

    private void onExportSnapshot(final ExportSnapshot exportSnapshot) {
        final Path snapshotDir = baseDirPath.resolve("snapshots");
        createDir(snapshotDir);

        final Path filePath = snapshotDir.resolve(exportSnapshot.id + "-snapshot.json");
        LOG.debug("Creating JSON file : {}", filePath);

        final NormalizedNode root = exportSnapshot.dataTreeCandidate.getRootNode().getDataAfter().get();
        checkState(root instanceof NormalizedNodeContainer, "Unexpected root %s", root);

        writeSnapshot(filePath, (NormalizedNodeContainer<?>) root);
        LOG.debug("Created JSON file: {}", filePath);
    }

    private void onExportJournal(final ExportJournal exportJournal) {
        entries.add(exportJournal.replicatedLogEntry);
    }

    private void onFinishExport(final FinishExport finishExport) {
        final Path journalDir = baseDirPath.resolve("journals");
        createDir(journalDir);

        final Path filePath = journalDir.resolve(finishExport.id + "-journal.json");
        LOG.debug("Creating JSON file : {}", filePath);
        writeJournal(filePath);
        LOG.debug("Created JSON file: {}", filePath);
    }

    private void writeSnapshot(final Path path, final NormalizedNodeContainer<?> root) {
        try (JsonWriter jsonWriter = new JsonWriter(Files.newBufferedWriter(path))) {
            jsonWriter.beginObject();

            try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(
                JSONNormalizedNodeStreamWriter.createNestedWriter(
                    JSONCodecFactorySupplier.RFC7951.getShared(schemaContext), SchemaPath.ROOT, null, jsonWriter),
                true)) {
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
            for (ReplicatedLogEntry entry : entries) {
                final Payload data = entry.getData();
                if (data instanceof CommitTransactionPayload) {
                    final CommitTransactionPayload payload = (CommitTransactionPayload) entry.getData();
                    final DataTreeCandidate candidate = payload.getCandidate().getValue().getCandidate();
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
        writer.beginObject();
        writer.name("Entry");
        writer.beginArray();
        doWriteNode(writer, candidate.getRootPath(), candidate.getRootNode());
        writer.endArray();
        writer.endObject();
    }

    private static void doWriteNode(final JsonWriter writer, final YangInstanceIdentifier path,
            final DataTreeCandidateNode node) throws IOException {
        switch (node.getModificationType()) {
            case APPEARED:
            case DISAPPEARED:
            case SUBTREE_MODIFIED:
                NodeIterator iterator = new NodeIterator(null, path, node.getChildNodes().iterator());
                do {
                    iterator = iterator.next(writer);
                } while (iterator != null);
                break;
            case DELETE:
            case UNMODIFIED:
            case WRITE:
                outputNodeInfo(writer, path, node);
                break;
            default:
                outputDefault(writer, path, node);
        }
    }

    private static void outputNodeInfo(final JsonWriter writer, final YangInstanceIdentifier path,
                                       final DataTreeCandidateNode node) throws IOException {
        final ModificationType modificationType = node.getModificationType();

        writer.beginObject().name("Node");
        writer.beginArray();
        writer.beginObject().name("Path").value(path.toString()).endObject();
        writer.beginObject().name("ModificationType").value(modificationType.toString()).endObject();
        if (modificationType == ModificationType.WRITE) {
            writer.beginObject().name("Data").value(node.getDataAfter().get().body().toString()).endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    private static void outputDefault(final JsonWriter writer, final YangInstanceIdentifier path,
                                      final DataTreeCandidateNode node) throws IOException {
        writer.beginObject().name("Node");
        writer.beginArray();
        writer.beginObject().name("Path").value(path.toString()).endObject();
        writer.beginObject().name("ModificationType")
                .value("UNSUPPORTED MODIFICATION: " + node.getModificationType()).endObject();
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
                final DataTreeCandidateNode node = iterator.next();
                final YangInstanceIdentifier child = path.node(node.getIdentifier());

                switch (node.getModificationType()) {
                    case APPEARED:
                    case DISAPPEARED:
                    case SUBTREE_MODIFIED:
                        return new NodeIterator(this, child, node.getChildNodes().iterator());
                    case DELETE:
                    case UNMODIFIED:
                    case WRITE:
                        outputNodeInfo(writer, path, node);
                        break;
                    default:
                        outputDefault(writer, child, node);
                }
            }

            return parent;
        }
    }
}
