/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreNodeCompositeBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableAugmentationNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableOrderedLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableOrderedMapNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableUnkeyedListEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.AugmentationSchemaProxy;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;

public abstract class SchemaAwareApplyOperation implements ModificationApplyOperation {

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

    private static SchemaAwareApplyOperation fromListSchemaNode(final ListSchemaNode schemaNode) {
        List<QName> keyDefinition = schemaNode.getKeyDefinition();
        if (keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListModificationStrategy(schemaNode);
        }
        if (schemaNode.isUserOrdered()) {
            return new OrderedMapModificationStrategy(schemaNode);
        }

        return new UnorderedMapModificationStrategy(schemaNode);
    }

    private static SchemaAwareApplyOperation fromLeafListSchemaNode(final LeafListSchemaNode schemaNode) {
        if(schemaNode.isUserOrdered()) {
            return new OrderedLeafSetModificationStrategy(schemaNode);
        } else {
            return new UnorderedLeafSetModificationStrategy(schemaNode);
        }
    }


    public static SchemaAwareApplyOperation from(final DataNodeContainer resolvedTree,
            final AugmentationTarget augSchemas, final AugmentationIdentifier identifier) {
        AugmentationSchema augSchema = null;
        allAugments: for (AugmentationSchema potential : augSchemas.getAvailableAugmentations()) {
            boolean containsAll = true;
            for (DataSchemaNode child : potential.getChildNodes()) {
                if (identifier.getPossibleChildNames().contains(child.getQName())) {
                    augSchema = potential;
                    break allAugments;
                }
            }
        }
        if (augSchema != null) {
            return new AugmentationModificationStrategy(augSchema, resolvedTree);
        }
        return null;
    }

    protected final ModificationApplyOperation resolveChildOperation(final PathArgument child) {
        Optional<ModificationApplyOperation> potential = getChild(child);
        checkArgument(potential.isPresent(), "Operation for child %s is not defined.", child);
        return potential.get();
    }

    @Override
    public void verifyStructure(final NodeModification modification) throws IllegalArgumentException {
        if (modification.getModificationType() == ModificationType.WRITE) {
            verifyWritenStructure(modification.getWritenValue());
        }
    }

    protected abstract void verifyWritenStructure(NormalizedNode<?, ?> writenValue);

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

    protected static final void checkNotConflicting(final InstanceIdentifier path,final StoreMetadataNode original, final StoreMetadataNode current) throws DataPreconditionFailedException {
        checkDataPrecondition(path, original.getNodeVersion().equals(current.getNodeVersion()),"Node was replaced by other transaction.");
        checkDataPrecondition(path,original.getSubtreeVersion().equals(current.getSubtreeVersion()), "Node children was modified by other transaction");
    }

    protected abstract void checkSubtreeModificationApplicable(InstanceIdentifier path,final NodeModification modification,
            final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException;

    private void checkDeleteApplicable(final NodeModification modification, final Optional<StoreMetadataNode> current) {
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

    public static abstract class ValueNodeModificationStrategy<T extends DataSchemaNode> extends
            SchemaAwareApplyOperation {

        private final T schema;
        private final Class<? extends NormalizedNode<?, ?>> nodeClass;

        protected ValueNodeModificationStrategy(final T schema, final Class<? extends NormalizedNode<?, ?>> nodeClass) {
            super();
            this.schema = schema;
            this.nodeClass = nodeClass;
        }

        @Override
        protected void verifyWritenStructure(final NormalizedNode<?, ?> writenValue) {
            checkArgument(nodeClass.isInstance(writenValue), "Node should must be of type %s", nodeClass);
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
            UnsignedLong nodeVersion = subtreeVersion;
            return StoreMetadataNode.builder().setNodeVersion(nodeVersion).setSubtreeVersion(subtreeVersion)
                    .setData(modification.getWritenValue()).build();
        }

        @Override
        protected void checkSubtreeModificationApplicable(final InstanceIdentifier path,final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            throw new DataPreconditionFailedException(path, "Subtree modification is not allowed.");
        }

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

    public static abstract class NormalizedNodeContainerModificationStrategy extends SchemaAwareApplyOperation {

        private final Class<? extends NormalizedNode<?, ?>> nodeClass;

        protected NormalizedNodeContainerModificationStrategy(final Class<? extends NormalizedNode<?, ?>> nodeClass) {
            this.nodeClass = nodeClass;
        }

        @Override
        public void verifyStructure(final NodeModification modification) throws IllegalArgumentException {
            if (modification.getModificationType() == ModificationType.WRITE) {

            }
            for (NodeModification childModification : modification.getModifications()) {
                resolveChildOperation(childModification.getIdentifier()).verifyStructure(childModification);
            }
        }

        @Override
        protected void checkWriteApplicable(final InstanceIdentifier path, final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            // FIXME: Implement proper write check for replacement of node container
            //        prerequisite is to have transaction chain available for clients
            //        otherwise this will break chained writes to same node.
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected void verifyWritenStructure(final NormalizedNode<?, ?> writenValue) {
            checkArgument(nodeClass.isInstance(writenValue), "Node should must be of type %s", nodeClass);
            checkArgument(writenValue instanceof NormalizedNodeContainer);
            NormalizedNodeContainer writenCont = (NormalizedNodeContainer) writenValue;
            for (Object child : writenCont.getValue()) {
                checkArgument(child instanceof NormalizedNode);
                NormalizedNode childNode = (NormalizedNode) child;
            }
        }

        @Override
        protected StoreMetadataNode applyWrite(final NodeModification modification,
                final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {

            NormalizedNode<?, ?> newValue = modification.getWritenValue();

            final UnsignedLong nodeVersion;
            if (currentMeta.isPresent()) {
                nodeVersion = StoreUtils.increase(currentMeta.get().getNodeVersion());
            } else {
                nodeVersion = subtreeVersion;
            }

            final StoreMetadataNode newValueMeta = StoreMetadataNode.createRecursively(newValue, nodeVersion, nodeVersion);
            if (!modification.hasAdditionalModifications()) {
                return newValueMeta;
            }

            @SuppressWarnings("rawtypes")
            NormalizedNodeContainerBuilder dataBuilder = createBuilder(newValue);
            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(dataBuilder) //
                    .setNodeVersion(nodeVersion) //
                    .setSubtreeVersion(subtreeVersion);

            return mutateChildren(modification.getModifications(), newValueMeta, builder, nodeVersion);
        }

        @Override
        protected StoreMetadataNode applyMerge(final NodeModification modification, final StoreMetadataNode currentMeta,
                final UnsignedLong subtreeVersion) {
            // For Node Containers - merge is same as subtree change - we only replace children.
            return applySubtreeChange(modification, currentMeta, subtreeVersion);
        }

        @Override
        public StoreMetadataNode applySubtreeChange(final NodeModification modification,
                final StoreMetadataNode currentMeta, final UnsignedLong subtreeVersion) {
            // Bump subtree version to its new target
            final UnsignedLong updatedSubtreeVersion = StoreUtils.increase(currentMeta.getSubtreeVersion());

            @SuppressWarnings("rawtypes")
            NormalizedNodeContainerBuilder dataBuilder = createBuilder(currentMeta.getData());
            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(dataBuilder, currentMeta)
                    .setIdentifier(modification.getIdentifier()).setNodeVersion(currentMeta.getNodeVersion())
                    .setSubtreeVersion(updatedSubtreeVersion);

            return mutateChildren(modification.getModifications(), currentMeta, builder, updatedSubtreeVersion);
        }

        private StoreMetadataNode mutateChildren(final Iterable<NodeModification> modifications, final StoreMetadataNode meta,
                final StoreNodeCompositeBuilder builder, final UnsignedLong nodeVersion) {

            for (NodeModification mod : modifications) {
                final PathArgument id = mod.getIdentifier();
                final Optional<StoreMetadataNode> cm = meta.getChild(id);

                Optional<StoreMetadataNode> result = resolveChildOperation(id).apply(mod, cm, nodeVersion);
                if (result.isPresent()) {
                    builder.add(result.get());
                } else {
                    builder.remove(id);
                }
            }

            return builder.build();
        }

        @Override
        protected void checkSubtreeModificationApplicable(final InstanceIdentifier path,final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            checkDataPrecondition(path, current.isPresent(), "Node was deleted by other transaction.");
            checkChildPreconditions(path,modification,current);

        }

        private void checkChildPreconditions(final InstanceIdentifier path, final NodeModification modification, final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            StoreMetadataNode currentMeta = current.get();
            for (NodeModification childMod : modification.getModifications()) {
                PathArgument childId = childMod.getIdentifier();
                Optional<StoreMetadataNode> childMeta = currentMeta.getChild(childId);
                InstanceIdentifier childPath = StoreUtils.append(path, childId);
                resolveChildOperation(childId).checkApplicable(childPath,childMod, childMeta);
            }
        }

        @Override
        protected void checkMergeApplicable(final InstanceIdentifier path, final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            if(current.isPresent()) {
                checkChildPreconditions(path,modification,current);
            }
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(NormalizedNode<?, ?> original);
    }

    public static abstract class DataNodeContainerModificationStrategy<T extends DataNodeContainer> extends
            NormalizedNodeContainerModificationStrategy {

        private final T schema;
        private final LoadingCache<PathArgument, ModificationApplyOperation> childCache = CacheBuilder.newBuilder()
                .build(CacheLoader.from(new Function<PathArgument, ModificationApplyOperation>() {

                    @Override
                    public ModificationApplyOperation apply(final PathArgument identifier) {
                        if (identifier instanceof AugmentationIdentifier && schema instanceof AugmentationTarget) {
                            return from(schema, (AugmentationTarget) schema, (AugmentationIdentifier) identifier);
                        }

                        DataSchemaNode child = schema.getDataChildByName(identifier.getNodeType());
                        if (child == null) {
                            return null;
                        }
                        return from(child);
                    }
                }));

        protected DataNodeContainerModificationStrategy(final T schema,
                final Class<? extends NormalizedNode<?, ?>> nodeClass) {
            super(nodeClass);
            this.schema = schema;
        }

        protected T getSchema() {
            return schema;
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            try {
                return Optional.<ModificationApplyOperation> fromNullable(childCache.get(identifier));
            } catch (ExecutionException e) {
                return Optional.absent();
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected abstract DataContainerNodeBuilder createBuilder(NormalizedNode<?, ?> original);

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [" + schema + "]";
        }

    }

    public static class ContainerModificationStrategy extends
            DataNodeContainerModificationStrategy<ContainerSchemaNode> {

        public ContainerModificationStrategy(final ContainerSchemaNode schemaNode) {
            super(schemaNode, ContainerNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof ContainerNode);
            return ImmutableContainerNodeBuilder.create((ContainerNode) original);
        }
    }

    public static class UnkeyedListItemModificationStrategy extends
            DataNodeContainerModificationStrategy<ListSchemaNode> {

        public UnkeyedListItemModificationStrategy(final ListSchemaNode schemaNode) {
            super(schemaNode, UnkeyedListEntryNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof UnkeyedListEntryNode);
            return ImmutableUnkeyedListEntryNodeBuilder.create((UnkeyedListEntryNode) original);
        }
    }

    public static class AugmentationModificationStrategy extends
            DataNodeContainerModificationStrategy<AugmentationSchema> {

        protected AugmentationModificationStrategy(final AugmentationSchema schema, final DataNodeContainer resolved) {
            super(createAugmentProxy(schema,resolved), AugmentationNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof AugmentationNode);
            return ImmutableAugmentationNodeBuilder.create((AugmentationNode) original);
        }
    }

    public static class ChoiceModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final ChoiceNode schema;
        private final Map<PathArgument, ModificationApplyOperation> childNodes;

        public ChoiceModificationStrategy(final ChoiceNode schemaNode) {
            super(org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode.class);
            this.schema = schemaNode;
            ImmutableMap.Builder<PathArgument, ModificationApplyOperation> child = ImmutableMap.builder();

            for (ChoiceCaseNode caze : schemaNode.getCases()) {
                for (DataSchemaNode cazeChild : caze.getChildNodes()) {
                    SchemaAwareApplyOperation childNode = from(cazeChild);
                    child.put(new NodeIdentifier(cazeChild.getQName()), childNode);
                }
            }
            childNodes = child.build();
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
            return Optional.fromNullable(childNodes.get(child));
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode);
            return ImmutableChoiceNodeBuilder.create((org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode) original);
        }
    }

    public static class ListEntryModificationStrategy extends DataNodeContainerModificationStrategy<ListSchemaNode> {

        protected ListEntryModificationStrategy(final ListSchemaNode schema) {
            super(schema, MapEntryNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected final DataContainerNodeBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof MapEntryNode);
            return ImmutableMapEntryNodeBuilder.create((MapEntryNode) original);
        }
    }

    public static class UnorderedLeafSetModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected UnorderedLeafSetModificationStrategy(final LeafListSchemaNode schema) {
            super((Class) LeafSetNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new LeafSetEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof LeafSetNode<?>);
            return ImmutableLeafSetNodeBuilder.create((LeafSetNode<?>) original);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            if (identifier instanceof NodeWithValue) {
                return entryStrategy;
            }
            return Optional.absent();
        }
    }

    public static class OrderedLeafSetModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected OrderedLeafSetModificationStrategy(final LeafListSchemaNode schema) {
            super((Class) LeafSetNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new LeafSetEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof OrderedLeafSetNode<?>);
            return ImmutableOrderedLeafSetNodeBuilder.create((OrderedLeafSetNode<?>) original);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            if (identifier instanceof NodeWithValue) {
                return entryStrategy;
            }
            return Optional.absent();
        }
    }

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
            return StoreMetadataNode.createRecursively(modification.getWritenValue(), subtreeVersion);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
            if (child instanceof NodeIdentifier) {
                return entryStrategy;
            }
            return Optional.absent();
        }

        @Override
        protected void verifyWritenStructure(final NormalizedNode<?, ?> writenValue) {

        }

        @Override
        protected void checkSubtreeModificationApplicable(final InstanceIdentifier path,final NodeModification modification,
                final Optional<StoreMetadataNode> current) throws DataPreconditionFailedException {
            throw new DataPreconditionFailedException(path, "Subtree modification is not allowed.");
        }

    }

    public static class UnorderedMapModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        protected UnorderedMapModificationStrategy(final ListSchemaNode schema) {
            super(MapNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new ListEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof MapNode);
            return ImmutableMapNodeBuilder.create((MapNode) original);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            if (identifier instanceof NodeIdentifierWithPredicates) {
                return entryStrategy;
            }
            return Optional.absent();
        }

        @Override
        public String toString() {
            return "UnorderedMapModificationStrategy [entry=" + entryStrategy + "]";
        }
    }

    public static class OrderedMapModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        protected OrderedMapModificationStrategy(final ListSchemaNode schema) {
            super(OrderedMapNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new ListEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final NormalizedNode<?, ?> original) {
            checkArgument(original instanceof OrderedMapNode);
            return ImmutableOrderedMapNodeBuilder.create((OrderedMapNode) original);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            if (identifier instanceof NodeIdentifierWithPredicates) {
                return entryStrategy;
            }
            return Optional.absent();
        }

        @Override
        public String toString() {
            return "OrderedMapModificationStrategy [entry=" + entryStrategy + "]";
        }
    }

    public void verifyIdentifier(final PathArgument identifier) {

    }

    public static AugmentationSchema createAugmentProxy(final AugmentationSchema schema, final DataNodeContainer resolved) {
        Set<DataSchemaNode> realChildSchemas = new HashSet<>();
        for(DataSchemaNode augChild : schema.getChildNodes()) {
            realChildSchemas.add(resolved.getDataChildByName(augChild.getQName()));
        }
        return new AugmentationSchemaProxy(schema, realChildSchemas);
    }

    public static boolean checkDataPrecondition(final InstanceIdentifier path, final boolean condition, final String message) throws DataPreconditionFailedException {
        if(!condition) {
            throw new DataPreconditionFailedException(path, message);
        }
        return condition;
    }

}
