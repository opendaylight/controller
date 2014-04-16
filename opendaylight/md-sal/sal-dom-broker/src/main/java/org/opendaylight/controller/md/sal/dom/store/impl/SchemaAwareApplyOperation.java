package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreNodeCompositeBuilder;
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
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.primitives.UnsignedLong;

public abstract class SchemaAwareApplyOperation implements ModificationApplyOperation {

    public static SchemaAwareApplyOperation from(final DataSchemaNode schemaNode) {
        if (schemaNode instanceof ContainerSchemaNode) {
            return new ContainerModificationStrategy((ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            return new ListMapModificationStrategy((ListSchemaNode) schemaNode);
        } else if (schemaNode instanceof ChoiceNode) {
            return new ChoiceModificationStrategy((ChoiceNode) schemaNode);
        } else if (schemaNode instanceof LeafListSchemaNode) {
            return new LeafSetEntryModificationStrategy((LeafListSchemaNode) schemaNode);
        } else if (schemaNode instanceof LeafSchemaNode) {
            return new LeafModificationStrategy((LeafSchemaNode) schemaNode);
        }
        throw new IllegalArgumentException("Not supported schema node type for " + schemaNode.getClass());
    }

    public static SchemaAwareApplyOperation from(final DataNodeContainer resolvedTree,
            final AugmentationTarget augSchemas, final AugmentationIdentifier identifier) {
        AugmentationSchema augSchema = null;
        allAugments : for (AugmentationSchema potential : augSchemas.getAvailableAugmentations()) {
            boolean containsAll = true;
            for(DataSchemaNode child : potential.getChildNodes()) {
                if(identifier.getPossibleChildNames().contains(child.getQName())) {
                    augSchema = potential;
                    break allAugments;
                }
            }
        }
        if(augSchema != null) {
            return new AugmentationModificationStrategy(augSchema,resolvedTree);
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
    public boolean isApplicable(final NodeModification modification, final Optional<StoreMetadataNode> current) {
        switch (modification.getModificationType()) {
        case DELETE:
            return isDeleteApplicable(modification, current);
        case SUBTREE_MODIFIED:
            return isSubtreeModificationApplicable(modification, current);
        case WRITE:
            return isWriteApplicable(modification, current);
        case UNMODIFIED:
            return true;
        default:
            return false;
        }
    }

    protected boolean isWriteApplicable(final NodeModification modification, final Optional<StoreMetadataNode> current) {
        Optional<StoreMetadataNode> original = modification.getOriginal();
        if (original.isPresent() && current.isPresent()) {
            return isNotConflicting(original.get(), current.get());
        } else if (current.isPresent()) {
            return false;
        }
        return true;

    }

    protected final boolean isNotConflicting(final StoreMetadataNode original, final StoreMetadataNode current) {
        return original.getNodeVersion().equals(current.getNodeVersion())
                && original.getSubtreeVersion().equals(current.getSubtreeVersion());
    }

    protected abstract boolean isSubtreeModificationApplicable(final NodeModification modification,
            final Optional<StoreMetadataNode> current);

    private boolean isDeleteApplicable(final NodeModification modification, final Optional<StoreMetadataNode> current) {
        // FiXME: Add delete conflict detection.
        return true;
    }

    @Override
    public final Optional<StoreMetadataNode> apply(final NodeModification modification,
            final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {

        switch (modification.getModificationType()) {
        case DELETE:
            return modification.storeSnapshot(Optional.<StoreMetadataNode>absent());
        case SUBTREE_MODIFIED:
            Preconditions.checkArgument(currentMeta.isPresent(),"Metadata not available for modification",modification);
            return modification.storeSnapshot(Optional.of(applySubtreeChange(modification, currentMeta.get(), subtreeVersion)));
        case WRITE:
            return modification.storeSnapshot(Optional.of(applyWrite(modification, currentMeta, subtreeVersion)));
        case UNMODIFIED:
            return currentMeta;
        default:
            throw new IllegalArgumentException("Provided modification type is not supported.");
        }
    }

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
        protected StoreMetadataNode applyWrite(final NodeModification modification,
                final Optional<StoreMetadataNode> currentMeta, final UnsignedLong subtreeVersion) {
            UnsignedLong nodeVersion = subtreeVersion;
            if (currentMeta.isPresent()) {
                nodeVersion = StoreUtils.increase(currentMeta.get().getNodeVersion());
            }

            return StoreMetadataNode.builder().setNodeVersion(nodeVersion).setSubtreeVersion(subtreeVersion)
                    .setData(modification.getWritenValue()).build();
        }

        @Override
        protected boolean isSubtreeModificationApplicable(final NodeModification modification,
                final Optional<StoreMetadataNode> current) {
            return false;
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
            //
            NormalizedNode<?, ?> newValue = modification.getWritenValue();

            UnsignedLong nodeVersion = subtreeVersion;
            if (currentMeta.isPresent()) {
                nodeVersion = StoreUtils.increase(currentMeta.get().getNodeVersion());
            }
            StoreMetadataNode newValueMeta = StoreMetadataNode.createRecursively(newValue, nodeVersion, nodeVersion);

            if (!modification.hasAdditionalModifications()) {
                return newValueMeta;
            }
            @SuppressWarnings("rawtypes")
            NormalizedNodeContainerBuilder dataBuilder = createBuilder(modification.getIdentifier());
            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(dataBuilder) //
                    .setNodeVersion(nodeVersion) //
                    .setSubtreeVersion(subtreeVersion);

            Set<PathArgument> processedPreexisting = applyPreexistingChildren(modification, newValueMeta.getChildren(),
                    builder, nodeVersion);
            applyNewChildren(modification, processedPreexisting, builder, nodeVersion);

            return builder.build();

        }

        @Override
        public StoreMetadataNode applySubtreeChange(final NodeModification modification,
                final StoreMetadataNode currentMeta, final UnsignedLong subtreeVersion) {

            UnsignedLong updatedSubtreeVersion = StoreUtils.increase(currentMeta.getSubtreeVersion());
            @SuppressWarnings("rawtypes")
            NormalizedNodeContainerBuilder dataBuilder = createBuilder(modification.getIdentifier());
            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(dataBuilder)
                    .setIdentifier(modification.getIdentifier()).setNodeVersion(currentMeta.getNodeVersion())
                    .setSubtreeVersion(updatedSubtreeVersion);
            // We process preexisting nodes
            Set<PathArgument> processedPreexisting = applyPreexistingChildren(modification, currentMeta.getChildren(),
                    builder, updatedSubtreeVersion);
            applyNewChildren(modification, processedPreexisting, builder, updatedSubtreeVersion);
            return builder.build();
        }

        private void applyNewChildren(final NodeModification modification, final Set<PathArgument> ignore,
                final StoreNodeCompositeBuilder builder, final UnsignedLong subtreeVersion) {
            for (NodeModification childModification : modification.getModifications()) {
                PathArgument childIdentifier = childModification.getIdentifier();
                // We skip allready processed modifications
                if (ignore.contains(childIdentifier)) {
                    continue;
                }

                builder.addIfPresent(resolveChildOperation(childIdentifier) //
                        .apply(childModification, Optional.<StoreMetadataNode> absent(), subtreeVersion));
            }
        }

        private Set<PathArgument> applyPreexistingChildren(final NodeModification modification,
                final Iterable<StoreMetadataNode> children, final StoreNodeCompositeBuilder nodeBuilder,
                final UnsignedLong subtreeVersion) {
            Builder<PathArgument> processedModifications = ImmutableSet.<PathArgument> builder();
            for (StoreMetadataNode childMeta : children) {
                PathArgument childIdentifier = childMeta.getIdentifier();
                // We retrieve Child modification metadata
                Optional<NodeModification> childModification = modification.getChild(childIdentifier);
                // Node is modified
                if (childModification.isPresent()) {
                    processedModifications.add(childIdentifier);
                    Optional<StoreMetadataNode> result = resolveChildOperation(childIdentifier) //
                            .apply(childModification.get(), Optional.of(childMeta), subtreeVersion);
                    nodeBuilder.addIfPresent(result);
                } else {
                    // Child is unmodified - reuse existing metadata and data
                    // snapshot
                    nodeBuilder.add(childMeta);
                }
            }
            return processedModifications.build();
        }

        @Override
        protected boolean isSubtreeModificationApplicable(final NodeModification modification,
                final Optional<StoreMetadataNode> current) {
            if (false == current.isPresent()) {
                return false;
            }
            boolean result = true;
            StoreMetadataNode currentMeta = current.get();
            for (NodeModification childMod : modification.getModifications()) {
                PathArgument childId = childMod.getIdentifier();
                Optional<StoreMetadataNode> childMeta = currentMeta.getChild(childId);
                result &= resolveChildOperation(childId).isApplicable(childMod, childMeta);
            }
            return result;
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(PathArgument identifier);
    }

    public static abstract class DataNodeContainerModificationStrategy<T extends DataNodeContainer> extends
            NormalizedNodeContainerModificationStrategy {

        private final T schema;
        private final LoadingCache<PathArgument, ModificationApplyOperation> childCache = CacheBuilder.newBuilder().build(
                CacheLoader.from(new Function<PathArgument, ModificationApplyOperation>() {

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
        protected abstract DataContainerNodeBuilder createBuilder(PathArgument identifier);

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
        protected DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            // TODO Auto-generated method stub
            checkArgument(identifier instanceof NodeIdentifier);
            return ImmutableContainerNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
        }

    }

    public static class AugmentationModificationStrategy extends
            DataNodeContainerModificationStrategy<AugmentationSchema> {

        protected AugmentationModificationStrategy(final AugmentationSchema schema, final DataNodeContainer resolved) {
            super(schema, AugmentationNode.class);
            // FIXME: Use resolved children instead of unresolved.

        }


        @Override
        protected DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            return Builders.augmentationBuilder().withNodeIdentifier((AugmentationIdentifier) identifier);
        }

    }

    public static class ChoiceModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final ChoiceNode schema;
        private final Map<PathArgument,ModificationApplyOperation> childNodes;

        public ChoiceModificationStrategy(final ChoiceNode schemaNode) {
            super(org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode.class);
            this.schema = schemaNode;
            ImmutableMap.Builder<PathArgument, ModificationApplyOperation> child = ImmutableMap.builder();

            for(ChoiceCaseNode caze : schemaNode.getCases()) {
                for(DataSchemaNode cazeChild : caze.getChildNodes()) {
                    SchemaAwareApplyOperation childNode = from(cazeChild);
                    child.put(new NodeIdentifier(cazeChild.getQName()),childNode);
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
        protected DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            checkArgument(identifier instanceof NodeIdentifier);
            return ImmutableChoiceNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
        }

    }

    public static class ListEntryModificationStrategy extends DataNodeContainerModificationStrategy<ListSchemaNode> {

        protected ListEntryModificationStrategy(final ListSchemaNode schema) {
            super(schema, MapEntryNode.class);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected final DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            return ImmutableMapEntryNodeBuilder.create().withNodeIdentifier((NodeIdentifierWithPredicates) identifier);
        }

    }

    public static class LeafSetModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected LeafSetModificationStrategy(final LeafListSchemaNode schema) {
            super((Class) LeafSetNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new LeafSetEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final PathArgument identifier) {
            return ImmutableLeafSetNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            if (identifier instanceof NodeWithValue) {
                return entryStrategy;
            }
            return Optional.absent();
        }

    }

    public static class ListMapModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        protected ListMapModificationStrategy(final ListSchemaNode schema) {
            super(MapNode.class);
            entryStrategy = Optional.<ModificationApplyOperation> of(new ListEntryModificationStrategy(schema));
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final PathArgument identifier) {
            return ImmutableMapNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
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
            return "ListMapModificationStrategy [entry=" + entryStrategy + "]";
        }
    }

    public void verifyIdentifier(final PathArgument identifier) {

    }

}
