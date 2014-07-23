/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.utils.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.datachange.notification.DataChangeListenerMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataChanged implements SerializableMessage {
    public static final Class SERIALIZABLE_CLASS =
        DataChangeListenerMessages.DataChanged.class;
    final private SchemaContext schemaContext;
    private final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>>
        change;



    public DataChanged(SchemaContext schemaContext,
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.change = change;
        this.schemaContext = schemaContext;
    }


    public AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }


    private NormalizedNodeMessages.Node convertToNodeTree(
        NormalizedNode<?, ?> normalizedNode) {

        return new NormalizedNodeToNodeCodec(schemaContext)
            .encode(YangInstanceIdentifier.builder().build(), normalizedNode)
            .getNormalizedNode();

    }

    private Iterable<NormalizedNodeMessages.InstanceIdentifier> convertToRemovePaths(
        Set<YangInstanceIdentifier> removedPaths) {
        final Set<NormalizedNodeMessages.InstanceIdentifier> removedPathInstanceIds = new HashSet<>();
        for (YangInstanceIdentifier id : removedPaths) {
            removedPathInstanceIds.add(InstanceIdentifierUtils.toSerializable(id));
        }
        return new Iterable<NormalizedNodeMessages.InstanceIdentifier>() {
            public Iterator<NormalizedNodeMessages.InstanceIdentifier> iterator() {
                return removedPathInstanceIds.iterator();
            }
        };

    }

    private NormalizedNodeMessages.NodeMap convertToNodeMap(
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> data) {
        NormalizedNodeToNodeCodec normalizedNodeToNodeCodec =
            new NormalizedNodeToNodeCodec(schemaContext);
        NormalizedNodeMessages.NodeMap.Builder nodeMapBuilder =
            NormalizedNodeMessages.NodeMap.newBuilder();
        NormalizedNodeMessages.NodeMapEntry.Builder builder =
            NormalizedNodeMessages.NodeMapEntry.newBuilder();
        for (Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : data
            .entrySet()) {


            NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
                InstanceIdentifierUtils.toSerializable(entry.getKey());

            builder.setInstanceIdentifierPath(instanceIdentifier)
                .setNormalizedNode(normalizedNodeToNodeCodec
                    .encode(entry.getKey(), entry.getValue())
                    .getNormalizedNode());
            nodeMapBuilder.addMapEntries(builder.build());
        }
        return nodeMapBuilder.build();
    }


    @Override
    public Object toSerializable() {
        return DataChangeListenerMessages.DataChanged.newBuilder()
            .addAllRemovedPaths(convertToRemovePaths(change.getRemovedPaths()))
            .setCreatedData(convertToNodeMap(change.getCreatedData()))
            .setOriginalData(convertToNodeMap(change.getOriginalData()))
            .setUpdatedData(convertToNodeMap(change.getUpdatedData()))
            .setOriginalSubTree(convertToNodeTree(change.getOriginalSubtree()))
            .setUpdatedSubTree(convertToNodeTree(change.getUpdatedSubtree()))
            .build();
    }

    public static DataChanged fromSerialize(SchemaContext sc, Object message,
        YangInstanceIdentifier pathId) {
        DataChangeListenerMessages.DataChanged dataChanged =
            (DataChangeListenerMessages.DataChanged) message;
        DataChangedEvent event = new DataChangedEvent(sc);
        if (dataChanged.getCreatedData() != null && dataChanged.getCreatedData()
            .isInitialized()) {
            event.setCreatedData(dataChanged.getCreatedData());
        }
        if (dataChanged.getOriginalData() != null && dataChanged
            .getOriginalData().isInitialized()) {
            event.setOriginalData(dataChanged.getOriginalData());
        }

        if (dataChanged.getUpdatedData() != null && dataChanged.getUpdatedData()
            .isInitialized()) {
            event.setUpdateData(dataChanged.getUpdatedData());
        }

        if (dataChanged.getOriginalSubTree() != null && dataChanged
            .getOriginalSubTree().isInitialized()) {
            event.setOriginalSubtree(dataChanged.getOriginalSubTree(), pathId);
        }

        if (dataChanged.getUpdatedSubTree() != null && dataChanged
            .getUpdatedSubTree().isInitialized()) {
            event.setUpdatedSubtree(dataChanged.getOriginalSubTree(), pathId);
        }

        if (dataChanged.getRemovedPathsList() != null && !dataChanged
            .getRemovedPathsList().isEmpty()) {
            event.setRemovedPaths(dataChanged.getRemovedPathsList());
        }

        return new DataChanged(sc, event);

    }

    static class DataChangedEvent implements
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> {
        private final SchemaContext schemaContext;
        private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData;
        private final NormalizedNodeToNodeCodec nodeCodec;
        private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> updatedData;
        private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> originalData;
        private NormalizedNode<?, ?> originalSubTree;
        private NormalizedNode<?, ?> updatedSubTree;
        private Set<YangInstanceIdentifier> removedPathIds;

        DataChangedEvent(SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
            nodeCodec = new NormalizedNodeToNodeCodec(schemaContext);
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            if(createdData == null){
                return Collections.emptyMap();
            }
            return createdData;
        }

        DataChangedEvent setCreatedData(
            NormalizedNodeMessages.NodeMap nodeMap) {
            this.createdData = convertNodeMapToMap(nodeMap);
            return this;
        }

        private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> convertNodeMapToMap(
            NormalizedNodeMessages.NodeMap nodeMap) {
            Map<YangInstanceIdentifier, NormalizedNode<?, ?>> mapEntries =
                new HashMap<YangInstanceIdentifier, NormalizedNode<?, ?>>();
            for (NormalizedNodeMessages.NodeMapEntry nodeMapEntry : nodeMap
                .getMapEntriesList()) {
                YangInstanceIdentifier id = InstanceIdentifierUtils
                    .fromSerializable(nodeMapEntry.getInstanceIdentifierPath());
                mapEntries.put(id,
                    nodeCodec.decode(id, nodeMapEntry.getNormalizedNode()));
            }
            return mapEntries;
        }


        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            if(updatedData == null){
                return Collections.emptyMap();
            }
            return updatedData;
        }

        DataChangedEvent setUpdateData(NormalizedNodeMessages.NodeMap nodeMap) {
            this.updatedData = convertNodeMapToMap(nodeMap);
            return this;
        }

        @Override
        public Set<YangInstanceIdentifier> getRemovedPaths() {
            if (removedPathIds == null) {
                return Collections.emptySet();
            }
            return removedPathIds;
        }

        public DataChangedEvent setRemovedPaths(List<NormalizedNodeMessages.InstanceIdentifier> removedPaths) {
            Set<YangInstanceIdentifier> removedIds = new HashSet<>();
            for (NormalizedNodeMessages.InstanceIdentifier path : removedPaths) {
                removedIds.add(InstanceIdentifierUtils.fromSerializable(path));
            }
            this.removedPathIds = removedIds;
            return this;
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
            if (originalData == null) {
                Collections.emptyMap();
            }
            return originalData;
        }

        DataChangedEvent setOriginalData(
            NormalizedNodeMessages.NodeMap nodeMap) {
            this.originalData = convertNodeMapToMap(nodeMap);
            return this;
        }

        @Override
        public NormalizedNode<?, ?> getOriginalSubtree() {
            return originalSubTree;
        }

        DataChangedEvent setOriginalSubtree(NormalizedNodeMessages.Node node,
            YangInstanceIdentifier instanceIdentifierPath) {
            originalSubTree = nodeCodec.decode(instanceIdentifierPath, node);
            return this;
        }

        @Override
        public NormalizedNode<?, ?> getUpdatedSubtree() {
            return updatedSubTree;
        }

        DataChangedEvent setUpdatedSubtree(NormalizedNodeMessages.Node node,
            YangInstanceIdentifier instanceIdentifierPath) {
            updatedSubTree = nodeCodec.decode(instanceIdentifierPath, node);
            return this;
        }


    }



}
