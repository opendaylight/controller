/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import akka.actor.Props;
import akka.persistence.SnapshotOffer;
import com.google.gson.stream.JsonWriter;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLog;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;


public class JsonExportActor extends AbstractUntypedActor {

    // Internal messages
    public static final class ExportSnapshot {
        private SnapshotOffer snapshotOffer;

        public ExportSnapshot(SnapshotOffer snapshotOffer) {
            this.snapshotOffer = snapshotOffer;
        }
    }

    public static final class ExportJournal {
        private ReplicatedLogEntry replicatedLogEntry;

        private String id;

        public ExportJournal(ReplicatedLogEntry replicatedLogEntry, String id) {
            this.replicatedLogEntry = replicatedLogEntry;
            this.id = id;
        }
    }

    public static final class FinishExport {
        private String id;

        public FinishExport(String id) {
            this.id = id;
        }
    }

    private RaftActorContext context;

    private String defaultDirPath = "${karaf.home}/json/";

    private File defaultDir = new File(defaultDirPath);

    private Map<String,List<ReplicatedLogEntry>> entries = new HashMap<>();

    private SchemaContext schemaContext;

    private JsonExportActor(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof ExportSnapshot) {
           // onExportSnapshot(((ExportSnapshot) message));
        } if (message instanceof ExportJournal) {
            onExportJournal(((ExportJournal) message));
        } if (message instanceof FinishExport) {
            onFinishExport((FinishExport)message);
        } else {
            unknownMessage(message);
        }
    }

    private void onExportSnapshot(ExportSnapshot exportSnapshot) {
        SnapshotOffer snapshotOffer = exportSnapshot.snapshotOffer;
        Snapshot snapshot = (Snapshot) snapshotOffer.snapshot();
        ReplicatedLog replicatedLog = context.getReplicatedLog();
        LOG.error(snapshot.toString());
        try (JsonWriter jsonWriter = createWriter("snapshot")) {
            jsonWriter.beginObject();
            jsonWriter.name("size").value(replicatedLog.size());
            jsonWriter.name("entries");
            jsonWriter.beginArray();
            for (int i = 0; i<=snapshot.getLastIndex(); i++) {
                //jsonWriter.value(replicatedLog.get(i).toString());
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        } catch (IOException e) {
            LOG.error("Unable to create file: " + e);
        }
    }

    private void onExportJournal(ExportJournal exportJournal) {
        String id = exportJournal.id;
        if (!entries.containsKey(id)) {
            entries.put(id, new ArrayList<>());
        }
        entries.get(id).add(exportJournal.replicatedLogEntry);
    }

    private void onFinishExport(FinishExport finishExport) {
        try {
            if (entries.containsKey(finishExport.id)) {
                JsonWriter writer = createWriter(finishExport.id + "-journal");
                List<ReplicatedLogEntry> journalEntries = entries.get(finishExport.id);
                writer.beginObject();
                writer.name("entries");
                writer.beginArray();
                for (ReplicatedLogEntry entry: journalEntries) {
                    if (entry.getData() instanceof CommitTransactionPayload) {
                        CommitTransactionPayload payload = (CommitTransactionPayload) entry.getData();
                        DataTreeCandidate candidate = payload.getCandidate().getValue().getCandidate();
                        writer.beginObject();
                        writer.name("entry");
                        DataTreeCandidateNode current = candidate.getRootNode();
                        writer.value(getNodes(Arrays.asList(current)).toString());
                        writer.beginArray();
                        getNodes(Arrays.asList(current)).forEach(e-> {
                            try {
                                writer.name("node");
                                writer.value(e.getDataAfter().get().getValue().toString());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
                        writer.endArray();
                        writer.endObject();
                        //writeNode(writer, candidate.getRootNode().getDataAfter().get());
                    }
                }
                writer.endArray();
                writer.endObject();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private List<DataTreeCandidateNode> getNodes(List<DataTreeCandidateNode> node) {
        List<DataTreeCandidateNode> nodes = new ArrayList<>(node);
        node.forEach(e-> {
            nodes.addAll(e.getChildNodes());
        });
        if (node.size() == nodes.size() || nodes.size()>100) {
            return getNormalizedNodes(nodes);
        } else {
            return getNodes(nodes);
        }
    }

    private List<DataTreeCandidateNode> getNormalizedNodes(List<DataTreeCandidateNode> node) {
        List<DataTreeCandidateNode> nodes = new ArrayList<>();
        node.forEach(e-> {
            if (e.getClass().getSimpleName().equals("NormalizedNodeDataTreeCandidateNode")) {
                nodes.add(e);
            }
        });
        return nodes;
    }


    private JsonWriter createWriter(String value) throws IOException {
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }

        final File filePath = Paths.get( value + ".json").toFile();
        LOG.info("Creating JSON file : {}", filePath);
        return new JsonWriter(new FileWriter(filePath));
    }

    public static Props props(SchemaContext schemaContext) {
        return Props.create(JsonExportActor.class, schemaContext);
    }

    private void writeNode(JsonWriter writer, NormalizedNode node) throws IOException {
        writer.beginObject();
        writer.name("entry");
        writer.value(node.getValue().toString());
        writer.endObject();
    }
}
