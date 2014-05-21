/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataPreconditionFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.DataNodeContainerModificationStrategy.ContainerModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.DataNodeContainerModificationStrategy.UnkeyedListItemModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.NormalizedNodeContainerModificationStrategy.ChoiceModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.NormalizedNodeContainerModificationStrategy.OrderedLeafSetModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.NormalizedNodeContainerModificationStrategy.OrderedMapModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.NormalizedNodeContainerModificationStrategy.UnorderedLeafSetModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.ValueNodeModificationStrategy.LeafModificationStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

abstract class SchemaAwareApplyOperation implements ModificationApplyOperation {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaAwareApplyOperation.class);

    public static SchemaAwareApplyOperation from(final DataSchemaNode schemaNode) {
        if (schemaNode instanceof ContainerSchemaNode) {
            return new ContainerModificationStrategy((ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            return fromListSchemaNode((ListSchemaNode) schemaNode);
        } else if (schemaNode instanceof ChoiceNode) {
            return new ChoiceModificationStrategy((ChoiceNode) schemaNode);
        } else if (schemaNode instanceof LeafListSchemaNode) {
            return fromLeafListSchemaNode((LeafListSchemaNode) schemaNode);
        } else if (schemaNode instanceof LeafSchemaNode) {
            return new LeafModificationStrategy((LeafSchemaNode) schemaNode);
        }
        throw new IllegalArgumentException("Not supported schema node type for " + schemaNode.getClass());
    }

    public static SchemaAwareApplyOperation from(final DataNodeContainer resolvedTree,
            final AugmentationTarget augSchemas, final AugmentationIdentifier identifier) {
        AugmentationSchema augSchema = null;

        allAugments:
            for (AugmentationSchema potential : augSchemas.getAvailableAugmentations()) {
                for (DataSchemaNode child : potential.getChildNodes()) {
                    if (identifier.getPossibleChildNames().contains(child.getQName())) {
                        augSchema = potential;
                        break allAugments;
                    }
                }
            }

        if (augSchema != null) {
            return new DataNodeContainerModificationStrategy.AugmentationModificationStrategy(augSchema, resolvedTree);
        }
        return null;
    }

    private static SchemaAwareApplyOperation fromListSchemaNode(final ListSchemaNode schemaNode) {
        List<QName> keyDefinition = schemaNode.getKeyDefinition();
        if (keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListModificationStrategy(schemaNode);
        }
        if (schemaNode.isUserOrdered()) {
            return new OrderedMapModificationStrategy(schemaNode);
        }

        return new NormalizedNodeContainerModificationStrategy.UnorderedMapModificationStrategy(schemaNode);
    }

    private static SchemaAwareApplyOperation fromLeafListSchemaNode(final LeafListSchemaNode schemaNode) {
        if(schemaNode.isUserOrdered()) {
            return new OrderedLeafSetModificationStrategy(schemaNode);
        } else {
            return new UnorderedLeafSetModificationStrategy(schemaNode);
        }
    }

    private static final void checkNotConflicting(final InstanceIdentifier path,final StoreMetadataNode original, final StoreMetadataNode current) throws DataPreconditionFailedException {
        checkDataPrecondition(path, original.getNodeVersion().equals(current.getNodeVersion()),"Node was replaced by other transaction.");
        checkDataPrecondition(path,original.getSubtreeVersion().equals(current.getSubtreeVersion()), "Node children was modified by other transaction");
    }

    protected final ModificationApplyOperation resolveChildOperation(final PathArgument child) {
        Optional<ModificationApplyOperation> potential = getChild(child);
        checkArgument(potential.isPresent(), "Operation for child %s is not defined.", child);
        return potential.get();
    }

    @Override
    public void verifyStructure(final NodeModification modification) throws IllegalArgumentException {
        if (modification.getModificationType() == ModificationType.WRITE) {
            verifyWrittenStructure(modification.getWrittenValue());
        }
    }

    @Override
    public void checkApplicable(final InstanceIdentifier path,final NodeModification modification, final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
        switch (modification.getModificationType()) {
        case DELETE:
            checkDeleteApplicable(modification, current);
        case SUBTREE_MODIFIED:
            checkSubtreeModificationApplicable(path,modification, current);
            return;
        case WRITE:
            checkWriteApplicable(path,modification, current);
            return;
        case MERGE:
            checkMergeApplicable(path,modification,current);
            return;
        case UNMODIFIED:
            return;
        default:
            throw new UnsupportedOperationException("Suplied modification type "+modification.getModificationType()+ "is not supported.");
        }

    }

    protected void checkMergeApplicable(final InstanceIdentifier path,final NodeModification modification, final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
        Optional<StoreMetadataNode> original = modification.getOriginal();
        if (original.isPresent() && current.isPresent()) {
            /*
             * We need to do conflict detection only and only if the value of leaf changed
             * before two transactions. If value of leaf is unchanged between two transactions
             * it should not cause transaction to fail, since result of this merge
             * leads to same data.
             */
            if(!original.get().getData().equals(current.get().getData())) {

                checkNotConflicting(path,original.get(), current.get());
            }
        }
    }

    protected void checkWriteApplicable(final InstanceIdentifier path,final NodeModification modification, final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
        Optional<StoreMetadataNode> original = modification.getOriginal();
        if (original.isPresent() && current.isPresent()) {
            checkNotConflicting(path,original.get(), current.get());
        } else if(original.isPresent()) {
            throw new DataPreconditionFailedException(path,"Node was deleted by other transaction.");
        }
    }

    private void checkDeleteApplicable(final NodeModification modification, final Optional<StoreMetadataNode> current) {
        // Delete is always applicable, we do not expose it to subclasses
        if (current.isPresent()) {
            LOG.trace("Delete operation turned to no-op on missing node {}", modification);
        }
    }

    @Override
    public final Optional<StoreMetadataNode> apply(final NodeModification modification,
            final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {

        switch (modification.getModificationType()) {
        case DELETE:
            return modification.storeSnapshot(Optional.<StoreMetadataNode> absent());
        case SUBTREE_MODIFIED:
            Preconditions.checkArgument(currentMeta.isPresent(), "Metadata not available for modification",
                    modification);
            return modification.storeSnapshot(Optional.of(applySubtreeChange(modification, currentMeta.get(),
                    subtreeVersion)));
        case MERGE:
            if(currentMeta.isPresent()) {
                return modification.storeSnapshot(Optional.of(applyMerge(modification,currentMeta.get(),subtreeVersion)));
            } // Fallback to write is intentional - if node is not preexisting merge is same as write
        case WRITE:
            return modification.storeSnapshot(Optional.of(applyWrite(modification, currentMeta, subtreeVersion)));
        case UNMODIFIED:
            return currentMeta;
        default:
            throw new IllegalArgumentException("Provided modification type is not supported.");
        }
    }

    protected abstract StoreMetadataNode applyMerge(NodeModification modification,
            StoreMetadataNode currentMeta, UnsignedLong subtreeVersion);

    protected abstract StoreMetadataNode applyWrite(NodeModification modification,
            Optional<StoreMetadataNode> currentMeta, UnsignedLong subtreeVersion);

    protected abstract StoreMetadataNode applySubtreeChange(NodeModification modification,
            StoreMetadataNode currentMeta, UnsignedLong subtreeVersion);

    protected abstract void checkSubtreeModificationApplicable(InstanceIdentifier path,final NodeModification modification,
            final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException;

    protected abstract void verifyWrittenStructure(NormalizedNode<?, ?> writtenValue);

    public static class UnkeyedListModificationStrategy extends SchemaAwareApplyOperation {

        private final Optional<ModificationApplyOperation> entryStrategy;

        protected UnkeyedListModificationStrategy(final ListSchemaNode schema) {
            entryStrategy = Optional.<ModificationApplyOperation> of(new UnkeyedListItemModificationStrategy(schema));
        }

        @Override
        protected StoreMetadataNode applyMerge(final NodeModification modification, final StoreMetadataNode currentMeta,
                final UnsignedLong subtreeVersion) {
            return applyWrite(modification, Optional.of(currentMeta), subtreeVersion);
        }

        @Override
        protected StoreMetadataNode applySubtreeChange(final NodeModification modification,
                final StoreMetadataNode currentMeta, final UnsignedLong subtreeVersion) {
            throw new UnsupportedOperationException("UnkeyedList does not support subtree change.");
        }

        @Override
        protected StoreMetadataNode applyWrite(final NodeModification modification,
                final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {
            return StoreMetadataNode.createRecursively(modification.getWrittenValue(), subtreeVersion);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
            if (child instanceof NodeIdentifier) {
                return entryStrategy;
            }
            return Optional.absent();
        }

        @Override
        protected void verifyWrittenStructure(final NormalizedNode<?, ?> writtenValue) {

        }

        @Override
        protected void checkSubtreeModificationApplicable(final InstanceIdentifier path,final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            throw new DataPreconditionFailedException(path, "Subtree modification is not allowed.");
        }
    }

    public static boolean checkDataPrecondition(final InstanceIdentifier path, final boolean condition, final String message) throws DataPreconditionFailedException {
        if(!condition) {
            throw new DataPreconditionFailedException(path, message);
        }
        return condition;
    }

}
