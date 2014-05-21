/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

 import static com.google.common.base.Preconditions.checkArgument;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataPreconditionFailedException;
 import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
 import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
 import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
 import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
 import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
 import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
 import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
 import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import com.google.common.base.Optional;
 import com.google.common.primitives.UnsignedLong;

 abstract class ValueNodeModificationStrategy<T extends DataSchemaNode> extends SchemaAwareApplyOperation {

     private final T schema;
     private final Class<? extends NormalizedNode<?, ?>> nodeClass;

     protected ValueNodeModificationStrategy(final T schema, final Class<? extends NormalizedNode<?, ?>> nodeClass) {
         super();
         this.schema = schema;
         this.nodeClass = nodeClass;
     }

     @Override
     protected void verifyWrittenStructure(final NormalizedNode<?, ?> writtenValue) {
         checkArgument(nodeClass.isInstance(writtenValue), "Node should must be of type %s", nodeClass);
     }

     @Override
     public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
         throw new UnsupportedOperationException("Node " + schema.getPath()
                 + "is leaf type node. Child nodes not allowed");
     }

     @Override
     protected StoreMetadataNode applySubtreeChange(final NodeModification modification,
             final StoreMetadataNode currentMeta, final UnsignedLong subtreeVersion) {
         throw new UnsupportedOperationException("Node " + schema.getPath()
                 + "is leaf type node. Subtree change is not allowed.");
     }

     @Override
     protected StoreMetadataNode applyMerge(final NodeModification modification, final StoreMetadataNode currentMeta,
             final UnsignedLong subtreeVersion) {
         return applyWrite(modification, Optional.of(currentMeta), subtreeVersion);
     }

     @Override
     protected StoreMetadataNode applyWrite(final NodeModification modification,
             final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {
         return StoreMetadataNode.builder(subtreeVersion).setSubtreeVersion(subtreeVersion)
                 .setData(modification.getWrittenValue()).build();
     }

     @Override
     protected void checkSubtreeModificationApplicable(final InstanceIdentifier path,final NodeModification modification,
             final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
         throw new DataPreconditionFailedException(path, "Subtree modification is not allowed.");
     }

     public static class LeafSetEntryModificationStrategy extends ValueNodeModificationStrategy<LeafListSchemaNode> {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         protected LeafSetEntryModificationStrategy(final LeafListSchemaNode schema) {
             super(schema, (Class) LeafSetEntryNode.class);
         }
     }

     public static class LeafModificationStrategy extends ValueNodeModificationStrategy<LeafSchemaNode> {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         protected LeafModificationStrategy(final LeafSchemaNode schema) {
             super(schema, (Class) LeafNode.class);
         }
     }
 }