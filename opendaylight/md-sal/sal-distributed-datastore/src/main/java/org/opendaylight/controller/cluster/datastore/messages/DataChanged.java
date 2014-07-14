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
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataChanged implements SerializableMessage{
  public static final Class SERIALIZABLE_CLASS = DataChangeListenerMessages.DataChanged.class;
    final private SchemaContext schemaContext;
    private final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>
        change;




    public DataChanged(SchemaContext schemaContext,
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.change = change;
        this.schemaContext = schemaContext;
    }


    public AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }


  private NormalizedNodeMessages.Node convertToNodeTree(NormalizedNode<?, ?> normalizedNode) {

      return new NormalizedNodeToNodeCodec(schemaContext).encode(InstanceIdentifier.builder().build(), normalizedNode).getNormalizedNode();

  }

  private Iterable<String> convertToRemovePaths(Set<InstanceIdentifier> removedPaths) {
    final Set<String>removedPathInstanceIds = new HashSet<String>();
    for(InstanceIdentifier id:removedPaths){
      removedPathInstanceIds.add(id.toString());
    }
    return new Iterable<String>() {
      public Iterator<String> iterator() {
        return removedPathInstanceIds.iterator();
      }
    };

  }

  private NormalizedNodeMessages.NodeMap convertToNodeMap(Map<InstanceIdentifier, NormalizedNode<?, ?>> data) {
    NormalizedNodeToNodeCodec normalizedNodeToNodeCodec = new NormalizedNodeToNodeCodec(schemaContext);
    NormalizedNodeMessages.NodeMap.Builder nodeMapBuilder = NormalizedNodeMessages.NodeMap.newBuilder();
    NormalizedNodeMessages.NodeMapEntry.Builder builder = NormalizedNodeMessages.NodeMapEntry.newBuilder();
    for(Map.Entry<InstanceIdentifier,NormalizedNode<?,?>> entry:data.entrySet()){

      builder.setInstanceIdentifierPath(entry.getKey().toString())
          .setNormalizedNode(normalizedNodeToNodeCodec.encode(entry.getKey(),entry.getValue()).getNormalizedNode());
      nodeMapBuilder.addMapEntries(builder.build());
    }
    return nodeMapBuilder.build();
  }


  @Override
  public Object toSerializable() {
    return DataChangeListenerMessages.DataChanged.newBuilder().addAllRemovedPaths(convertToRemovePaths(change.getRemovedPaths()))
                                        .setCreatedData(convertToNodeMap(change.getCreatedData()))
                                        .setOriginalData(convertToNodeMap(change.getOriginalData()))
                                         .setUpdatedData(convertToNodeMap(change.getUpdatedData()))
                                         .setOriginalSubTree(convertToNodeTree(change.getOriginalSubtree()))
                                         .setUpdatedSubTree(convertToNodeTree(change.getUpdatedSubtree()))
                                         .build();
  }

  public static DataChanged fromSerialize(SchemaContext sc, Object message){
    DataChangeListenerMessages.DataChanged dataChanged = (DataChangeListenerMessages.DataChanged)message;
    DataChangedEvent event = new DataChangedEvent(sc);
    if(dataChanged.getCreatedData() != null && dataChanged.getCreatedData().isInitialized()) {
      event.setCreatedData(dataChanged.getCreatedData()) ;
    }
    if(dataChanged.getOriginalData() != null && dataChanged.getOriginalData().isInitialized()) {
      event.setOriginalData(dataChanged.getOriginalData());
    }

    if(dataChanged.getUpdatedData() != null && dataChanged.getUpdatedData().isInitialized()) {
      event.setUpdateData(dataChanged.getUpdatedData());
    }

    if(dataChanged.getOriginalSubTree() != null && dataChanged.getOriginalSubTree().isInitialized()) {
      event.setOriginalSubtree(dataChanged.getOriginalSubTree());
    }

    if(dataChanged.getUpdatedSubTree() != null && dataChanged.getUpdatedSubTree().isInitialized()) {
      event.setUpdatedSubtree(dataChanged.getOriginalSubTree());
    }

    if(dataChanged.getRemovedPathsList() != null && !dataChanged.getRemovedPathsList().isEmpty()) {
      event.setRemovedPaths(dataChanged.getRemovedPathsList());
    }

     return new DataChanged(sc,event);

  }

  static class DataChangedEvent implements AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>{
    private final SchemaContext schemaContext;
    private Map<InstanceIdentifier, NormalizedNode<?, ?>>createdData;
    private final NormalizedNodeToNodeCodec nodeCodec;
    private Map<InstanceIdentifier, NormalizedNode<?, ?>> updatedData;
    private Map<InstanceIdentifier, NormalizedNode<?, ?>> originalData;
    private NormalizedNode<?,?>originalSubTree;
    private NormalizedNode<?,?>updatedSubTree;
    private Set<InstanceIdentifier> removedPathIds;

    DataChangedEvent(SchemaContext schemaContext){
      this.schemaContext = schemaContext;
      nodeCodec =  new NormalizedNodeToNodeCodec(schemaContext);
    }

    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
      return createdData;
    }
    DataChangedEvent setCreatedData(NormalizedNodeMessages.NodeMap nodeMap){
      this.createdData = convertNodeMapToMap(nodeMap);
      return this;
    }

    private Map<InstanceIdentifier, NormalizedNode<?, ?>> convertNodeMapToMap(NormalizedNodeMessages.NodeMap nodeMap) {
      Map<InstanceIdentifier, NormalizedNode<?, ?>>mapEntries = new HashMap<InstanceIdentifier, NormalizedNode<?, ?>>();
      for(NormalizedNodeMessages.NodeMapEntry nodeMapEntry:nodeMap.getMapEntriesList()){
          InstanceIdentifier id = InstanceIdentifierUtils.from(nodeMapEntry.getInstanceIdentifierPath());
          mapEntries.put(id,
             nodeCodec.decode(id,nodeMapEntry.getNormalizedNode()));
      }
      return mapEntries;
    }


    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
      return updatedData;
    }

    DataChangedEvent setUpdateData(NormalizedNodeMessages.NodeMap nodeMap){
      this.updatedData = convertNodeMapToMap(nodeMap);
      return this;
    }

    @Override
    public Set<InstanceIdentifier> getRemovedPaths() {
      return removedPathIds;
    }

    public DataChangedEvent setRemovedPaths(List<String> removedPaths) {
      Set<InstanceIdentifier> removedIds = new HashSet<>();
      for(String path: removedPaths){
        removedIds.add(InstanceIdentifierUtils.from(path));
      }
      this.removedPathIds = removedIds;
      return this;
    }

    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
      return originalData;
    }

    DataChangedEvent setOriginalData(NormalizedNodeMessages.NodeMap nodeMap){
      this.originalData = convertNodeMapToMap(nodeMap);
      return this;
    }

    @Override
    public NormalizedNode<?, ?> getOriginalSubtree() {
      return originalSubTree;
    }

    DataChangedEvent setOriginalSubtree(NormalizedNodeMessages.Node node){
       originalSubTree = nodeCodec.decode(InstanceIdentifier.builder().build(),node);
       return this;
    }

    @Override
    public NormalizedNode<?, ?> getUpdatedSubtree() {
      return updatedSubTree;
    }

    DataChangedEvent setUpdatedSubtree(NormalizedNodeMessages.Node node){
      updatedSubTree = nodeCodec.decode(InstanceIdentifier.builder().build(),node);
      return this;
    }


  }





}
