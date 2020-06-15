/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import akka.actor.Props;
import com.google.gson.stream.JsonWriter;
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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.util.Objects.requireNonNull;

public class JsonExportActor extends AbstractUntypedActor {

    // Internal messages
    public static final class ExportSnapshot {
        private String id;

        private DataTreeCandidate dataTreeCandidate;

        public ExportSnapshot(DataTreeCandidate candidate, String id) {
            this.dataTreeCandidate = candidate;
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

    private String defaultDirPath = "json";

    private File defaultDir = new File(defaultDirPath);

    private List<ReplicatedLogEntry> entries = new ArrayList<>();

    private SchemaContext schemaContext;

    private JsonExportActor(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof ExportSnapshot) {
            onExportSnapshot(((ExportSnapshot) message));
        } if (message instanceof ExportJournal) {
            onExportJournal(((ExportJournal) message));
        } if (message instanceof FinishExport) {
            onFinishExport((FinishExport)message);
        } else {
            unknownMessage(message);
        }
    }

    private void onExportSnapshot(ExportSnapshot exportSnapshot) {
        try (JsonWriter writer = createWriter(exportSnapshot.id + "-snapshot")) {
            NormalizedNode root = exportSnapshot.dataTreeCandidate.getRootNode().getDataAfter().get();
            writeSnapshot(writer,root);
        } catch (IOException e) {
            LOG.error("Error creating file: " + e.getMessage());
        }
    }

    private void writeSnapshot(JsonWriter writer, NormalizedNode root) {
        try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(
                JSONNormalizedNodeStreamWriter.createNestedWriter(
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(schemaContext),
                                SchemaPath.ROOT, null, writer), true)) {
            writer.beginObject();
            if (root instanceof NormalizedNodeContainer) {
                final Collection<NormalizedNode<?, ?>> nodes = (Collection<NormalizedNode<?, ?>>) root.getValue();
                for (NormalizedNode node: nodes) {
                    nnWriter.write(node);
                    nnWriter.flush();
                }
            } else {
                throw new IllegalStateException("Root node is not instance of NormalizedNodeContainer");
            }
            writer.endObject();
        } catch (IOException e) {
            LOG.error("Error creating Node Writer " + e.getMessage());
        }
    }

    private void onExportJournal(ExportJournal exportJournal) {
        entries.add(exportJournal.replicatedLogEntry);
    }

    private void onFinishExport(FinishExport finishExport) {
        try (JsonWriter writer = createWriter(finishExport.id + "-journal")) {
            writer.beginObject();
            writer.name("Entries");
            writer.beginArray();
            for (ReplicatedLogEntry entry: entries) {
                if (entry.getData() instanceof CommitTransactionPayload) {
                    CommitTransactionPayload payload = (CommitTransactionPayload) entry.getData();
                    DataTreeCandidate candidate = payload.getCandidate().getValue().getCandidate();
                    writeNode(writer, candidate);
                } else {
                    writer.beginObject().name("LogEntry").value(entry.getData().toString()).endObject();
                }
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            LOG.error("Error creating file: " + e.getMessage());
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
                writer.beginObject().name("Path").value(path.toString()).endObject();
                writer.beginObject().name("Modification").value("DELETE").endObject();
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
                writer.beginObject().name("Path").value(path.toString()).endObject();
                writer.beginObject().name("Modification").value("UNMODIFIED").endObject();
                writer.endArray();
                writer.endObject();
                break;
            case WRITE:
                writer.beginObject().name("Node");
                writer.beginArray();
                writer.beginObject().name("Path").value(path.toString()).endObject();
                writer.beginObject().name("Modification").value("WRITE").endObject();
                writer.beginObject().name("Value").value(node.getDataAfter().get().getValue().toString()).endObject();
                writer.endArray();
                writer.endObject();
                break;
            default:
                throw new IllegalArgumentException("Unsupported modification " + node.getModificationType());
        }
        writer.endArray();
        writer.endObject();
    }

    private JsonWriter createWriter(String value) throws IOException {
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }

        final File filePath = Paths.get( defaultDirPath + "/" + value + ".json").toFile();
        LOG.debug("Creating JSON file : {}", filePath);
        return new JsonWriter(new FileWriter(filePath));
    }

    public static Props props(SchemaContext schemaContext) {
        return Props.create(JsonExportActor.class, schemaContext);
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
                        writer.beginObject().name("Path").value(path.toString()).endObject();
                        writer.beginObject().name("Modification").value("DELETE").endObject();
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
                        writer.beginObject().name("Path").value(path.toString()).endObject();
                        writer.beginObject().name("Modification").value("UNMODIFIED").endObject();
                        writer.endArray();
                        writer.endObject();
                        break;
                    case WRITE:
                        writer.beginObject().name("Node");
                        writer.beginArray();
                        writer.beginObject().name("Path").value(path.toString()).endObject();
                        writer.beginObject().name("Modification").value("WRITE").endObject();
                        writer.beginObject().name("Value").value(node.getDataAfter().get().getValue().toString()).endObject();
                        writer.endArray();
                        writer.endObject();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported modification " + node.getModificationType());
                }
            }

            return parent;
        }
    }
}
