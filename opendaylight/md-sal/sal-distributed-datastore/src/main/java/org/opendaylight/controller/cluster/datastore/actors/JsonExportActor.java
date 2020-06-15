/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static java.util.Objects.requireNonNull;

import akka.actor.Props;
import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class JsonExportActor extends AbstractUntypedActor {

    // Internal messages
    public static final class ExportSnapshot {
        private final String id;

        private final DataTreeCandidate dataTreeCandidate;

        public ExportSnapshot(DataTreeCandidate candidate, String id) {
            this.dataTreeCandidate = requireNonNull(candidate);
            this.id = id;
        }
    }

    public static final class ExportJournal {
        private ReplicatedLogEntry replicatedLogEntry;

        public ExportJournal(ReplicatedLogEntry replicatedLogEntry) {
            this.replicatedLogEntry = replicatedLogEntry;
        }
    }

    public static final class FinishExport {
        private String id;

        public FinishExport(String id) {
            this.id = id;
        }
    }

    private String baseDirPath;

    private String snapshotDirPath;

    private String journalDirPath;

    private List<ReplicatedLogEntry> entries = new ArrayList<>();

    private EffectiveModelContext schemaContext;

    private JsonExportActor(EffectiveModelContext schemaContext, String dirPath) {
        this.schemaContext = schemaContext;
        this.baseDirPath = dirPath;
        snapshotDirPath = baseDirPath + "\\snapshots";
        journalDirPath = baseDirPath + "\\journals";
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof ExportSnapshot) {
            onExportSnapshot(((ExportSnapshot) message));
        } else if (message instanceof ExportJournal) {
            onExportJournal(((ExportJournal) message));
        } else if (message instanceof FinishExport) {
            onFinishExport((FinishExport)message);
        } else {
            unknownMessage(message);
        }
    }

    private void onExportSnapshot(ExportSnapshot exportSnapshot) {
        createSnapshotDir();
        final File filePath = Paths.get(snapshotDirPath + "\\" + exportSnapshot.id + "-snapshot.json").toFile();
        LOG.error("Creating JSON file : {}", filePath);
        NormalizedNode root = exportSnapshot.dataTreeCandidate.getRootNode().getDataAfter().get();
        writeSnapshot(filePath, root);
        LOG.error("Created JSON file: {}", filePath);
    }

    private void onExportJournal(ExportJournal exportJournal) {
        entries.add(exportJournal.replicatedLogEntry);
    }

    private void onFinishExport(FinishExport finishExport) {
        createJournalDir();
        final File filePath = Paths.get(journalDirPath + "\\" + finishExport.id + "-journal.json").toFile();
        LOG.error("Creating JSON file : {}", filePath);
        writeJournal(filePath);
        LOG.error("Created JSON file: {}", filePath);
    }

    private void writeSnapshot(File filePath, NormalizedNode root) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             Writer fileWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(fileWriter);
             NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(
                     JSONNormalizedNodeStreamWriter.createNestedWriter(
                             JSONCodecFactorySupplier.RFC7951.getShared(schemaContext),
                             SchemaPath.ROOT,
                             null,
                             jsonWriter),
                     true)) {
            Preconditions.checkState(root instanceof NormalizedNodeContainer,
                    "Root node is not instance of NormalizedNodeContainer");
            jsonWriter.beginObject();
            final Collection<NormalizedNode<?, ?>> nodes = (Collection<NormalizedNode<?, ?>>) root.getValue();
            for (NormalizedNode node : nodes) {
                nnWriter.write(node);
            }
            jsonWriter.endObject();
        } catch (IOException e) {
            LOG.error("Error during snapshot export: {} ", e.getMessage());
        }
    }

    private void writeJournal(File filePath) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             Writer fileWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(fileWriter)) {
            jsonWriter.beginObject();
            jsonWriter.name("Entries");
            jsonWriter.beginArray();
            for (ReplicatedLogEntry entry: entries) {
                if (entry.getData() instanceof CommitTransactionPayload) {
                    CommitTransactionPayload payload = (CommitTransactionPayload) entry.getData();
                    DataTreeCandidate candidate = payload.getCandidate().getValue().getCandidate();
                    writeNode(jsonWriter, candidate);
                } else {
                    jsonWriter.beginObject().name("Payload").value(entry.getData() + "").endObject();
                }
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        }
        catch (IOException e) {
            LOG.error("Error during journal export: {} ", e.getMessage());
        }
    }

    private void writeNode(JsonWriter writer, DataTreeCandidate candidate) throws IOException {
        writer.beginObject();
        writer.name("Entry");
        writer.beginArray();
        final DataTreeCandidateNode node = candidate.getRootNode();
        final YangInstanceIdentifier path = candidate.getRootPath();
        switch (node.getModificationType()) {
            case DELETE:
                writer.beginObject().name("Node");
                writer.beginArray();
                writer.beginObject().name("Path").value(path + "").endObject();
                writer.beginObject().name("ModificationType").value("DELETE").endObject();
                writer.endArray();
                writer.endObject();
                break;
            case SUBTREE_MODIFIED:
                NodeIterator iterator = new NodeIterator(null, path, node.getChildNodes().iterator());
                do {
                    iterator = iterator.next(writer);
                } while (iterator != null);
                break;
            case UNMODIFIED:
                writer.beginObject().name("Node");
                writer.beginArray();
                writer.beginObject().name("Path").value(path + "").endObject();
                writer.beginObject().name("ModificationType").value("UNMODIFIED").endObject();
                writer.endArray();
                writer.endObject();
                break;
            case WRITE:
                writer.beginObject().name("Node");
                writer.beginArray();
                writer.beginObject().name("Path").value(path + "").endObject();
                writer.beginObject().name("ModificationType").value("WRITE").endObject();
                writer.beginObject().name("Data").value(node.getDataAfter().get().getValue() + "").endObject();
                writer.endArray();
                writer.endObject();
                break;
            default:
                writer.beginObject().name("Node");
                writer.beginArray();
                writer.beginObject().name("Path").value(path + "").endObject();
                writer.beginObject().name("ModificationType")
                        .value("UNSUPPORTED MODIFICATION: " + node.getModificationType()).endObject();
                writer.endArray();
                writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    private void createDir(String path) {
        try {
            Files.createDirectory(Path.of(path));
        } catch (IOException e) {
            LOG.warn("Directory {} cannot be created.", path);
        }
    }

    private void createJournalDir() {
        createDir(baseDirPath);
        createDir(journalDirPath);
    }

    private void createSnapshotDir() {
        createDir(baseDirPath);
        createDir(snapshotDirPath);
    }

    public static Props props(EffectiveModelContext schemaContext, String dirPath) {
        return Props.create(JsonExportActor.class, schemaContext, dirPath);
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
                    case DELETE:
                        writer.beginObject().name("Node");
                        writer.beginArray();
                        writer.beginObject().name("Path").value(path + "").endObject();
                        writer.beginObject().name("ModificationType").value("DELETE").endObject();
                        writer.endArray();
                        writer.endObject();
                        break;
                    case APPEARED:
                    case DISAPPEARED:
                    case SUBTREE_MODIFIED:
                        return new NodeIterator(this, child, node.getChildNodes().iterator());
                    case UNMODIFIED:
                        writer.beginObject().name("Node");
                        writer.beginArray();
                        writer.beginObject().name("Path").value(path + "").endObject();
                        writer.beginObject().name("ModificationType").value("UNMODIFIED").endObject();
                        writer.endArray();
                        writer.endObject();
                        break;
                    case WRITE:
                        writer.beginObject().name("Node");
                        writer.beginArray();
                        writer.beginObject().name("Path").value(path + "").endObject();
                        writer.beginObject().name("ModificationType").value("WRITE").endObject();
                        writer.beginObject().name("Data").value(node.getDataAfter().get().getValue() + "")
                                .endObject();
                        writer.endArray();
                        writer.endObject();
                        break;
                    default:
                        writer.beginObject().name("Node");
                        writer.beginArray();
                        writer.beginObject().name("Path").value(path + "").endObject();
                        writer.beginObject().name("ModificationType")
                                .value("UNSUPPORTED MODIFICATION: " + node.getModificationType()).endObject();
                        writer.endArray();
                        writer.endObject();
                }
            }

            return parent;
        }
    }
}
