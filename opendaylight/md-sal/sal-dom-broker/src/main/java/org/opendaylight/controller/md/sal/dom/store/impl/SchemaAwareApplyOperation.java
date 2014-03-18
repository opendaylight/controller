package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Set;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreNodeCompositeBuilder;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Optional;
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

    @Override
    public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
        throw new IllegalArgumentException();
    }

    protected final ModificationApplyOperation resolveChildOperation(final PathArgument child) {
        Optional<ModificationApplyOperation> potential = getChild(child);
        checkArgument(potential.isPresent(), "Operation for child %s is not defined.", child);
        return potential.get();
    }

    @Override
    public final Optional<StoreMetadataNode> apply(final NodeModification modification,
            final Optional<StoreMetadataNode> currentMeta) {
        switch (modification.getModificationType()) {
        case DELETE:
            return Optional.absent();
        case SUBTREE_MODIFIED:
            return Optional.of(applySubtreeChange(modification, currentMeta.get()));
        case WRITE:
            return Optional.of(applyWrite(modification, currentMeta));
        case UNMODIFIED:
            return currentMeta;
        default:
            throw new IllegalArgumentException("Provided modification type is not supported.");
        }
    }

    protected abstract StoreMetadataNode applyWrite(NodeModification modification,
            Optional<StoreMetadataNode> currentMeta);

    protected abstract StoreMetadataNode applySubtreeChange(NodeModification modification, StoreMetadataNode currentMeta);

    public static abstract class ValueNodeModificationStrategy<T extends DataSchemaNode> extends
            SchemaAwareApplyOperation {

        private final T schema;

        protected ValueNodeModificationStrategy(final T schema) {
            super();
            this.schema = schema;
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
            throw new UnsupportedOperationException("Node " + schema.getPath()
                    + "is leaf type node. Child nodes not allowed");
        }

        @Override
        protected StoreMetadataNode applySubtreeChange(final NodeModification modification, final StoreMetadataNode currentMeta) {
            throw new UnsupportedOperationException("Node " + schema.getPath()
                    + "is leaf type node. Subtree change is not allowed.");
        }

        @Override
        protected StoreMetadataNode applyWrite(final NodeModification modification, final Optional<StoreMetadataNode> currentMeta) {
            return StoreMetadataNode.builder()
            // FIXME Add .increaseNodeVersion()
                    .setData(modification.getWritenValue()).build();
        }

    }

    public static class LeafSetEntryModificationStrategy extends ValueNodeModificationStrategy<LeafListSchemaNode> {

        protected LeafSetEntryModificationStrategy(final LeafListSchemaNode schema) {
            super(schema);
        }
    }

    public static class LeafModificationStrategy extends ValueNodeModificationStrategy<LeafSchemaNode> {

        protected LeafModificationStrategy(final LeafSchemaNode schema) {
            super(schema);
        }
    }

    public static abstract class NormalizedNodeContainerModificationStrategy extends SchemaAwareApplyOperation {

        @Override
        protected StoreMetadataNode applyWrite(final NodeModification modification, final Optional<StoreMetadataNode> currentMeta) {
            //
            NormalizedNode<?, ?> newValue = modification.getWritenValue();

            StoreMetadataNode newValueMeta = StoreMetadataNode.createRecursivelly(newValue, UnsignedLong.valueOf(0));

            if(!modification.hasAdditionalModifications()) {
                return newValueMeta;
            }
            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(newValueMeta,
                    createBuilder(modification.getIdentifier()));

            Set<PathArgument> processedPreexisting = applyPreexistingChildren(modification, newValueMeta.getChildren(), builder);
            applyNewChildren(modification, processedPreexisting, builder);

            return builder.build();

        }

        @Override
        @SuppressWarnings("rawtypes")
        public StoreMetadataNode applySubtreeChange(final NodeModification modification, final StoreMetadataNode currentMeta) {

            StoreNodeCompositeBuilder builder = StoreNodeCompositeBuilder.from(currentMeta,
                    createBuilder(modification.getIdentifier()));
            builder.setIdentifier(modification.getIdentifier());

            // We process preexisting nodes
            Set<PathArgument> processedPreexisting = applyPreexistingChildren(modification,
                    currentMeta.getChildren(), builder);
            applyNewChildren(modification, processedPreexisting, builder);
            return builder.build();
        }

        private void applyNewChildren(final NodeModification modification, final Set<PathArgument> ignore,
                final StoreNodeCompositeBuilder builder) {
            for (NodeModification childModification : modification.getModifications()) {
                PathArgument childIdentifier = childModification.getIdentifier();
                // We skip allready processed modifications
                if (ignore.contains(childIdentifier)) {
                    continue;
                }
                Optional<StoreMetadataNode> childResult = resolveChildOperation(childIdentifier) //
                        .apply(childModification, Optional.<StoreMetadataNode> absent());
                if (childResult.isPresent()) {
                    builder.add(childResult.get());
                }
            }
        }

        private Set<PathArgument> applyPreexistingChildren(final NodeModification modification,
                final Iterable<StoreMetadataNode> children, final StoreNodeCompositeBuilder nodeBuilder) {
            Builder<PathArgument> processedModifications = ImmutableSet.<PathArgument> builder();
            for (StoreMetadataNode childMeta : children) {
                PathArgument childIdentifier = childMeta.getIdentifier();
                // We retrieve Child modification metadata
                Optional<NodeModification> childModification = modification.getChild(childIdentifier);
                // Node is modified
                if (childModification.isPresent()) {
                    processedModifications.add(childIdentifier);
                    Optional<StoreMetadataNode> change = resolveChildOperation(childIdentifier) //
                            .apply(childModification.get(), Optional.of(childMeta));
                } else {
                    // Child is unmodified - reuse existing metadata and data
                    // snapshot
                    nodeBuilder.add(childMeta);
                }
            }
            return processedModifications.build();
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(PathArgument identifier);
    }

    public static abstract class DataNodeContainerModificationStrategy<T extends DataNodeContainer> extends
            NormalizedNodeContainerModificationStrategy {

        private final T schema;

        protected DataNodeContainerModificationStrategy(final T schema) {
            super();
            this.schema = schema;
        }

        protected T getSchema() {
            return schema;
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument identifier) {
            DataSchemaNode child = schema.getDataChildByName(identifier.getNodeType());
            if (child == null || child.isAugmenting()) {
                return Optional.absent();
            }
            return Optional.<ModificationApplyOperation> of(from(child));
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
            super(schemaNode);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            // TODO Auto-generated method stub
            checkArgument(identifier instanceof NodeIdentifier);
            return ImmutableContainerNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
        }

    }

    public static class ChoiceModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final ChoiceNode schema;

        public ChoiceModificationStrategy(final ChoiceNode schemaNode) {
            this.schema = schemaNode;
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            checkArgument(identifier instanceof NodeIdentifier);
            return ImmutableContainerNodeBuilder.create().withNodeIdentifier((NodeIdentifier) identifier);
        }

    }

    public static class ListEntryModificationStrategy extends DataNodeContainerModificationStrategy<ListSchemaNode> {

        protected ListEntryModificationStrategy(final ListSchemaNode schema) {
            super(schema);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected final DataContainerNodeBuilder createBuilder(final PathArgument identifier) {
            return ImmutableMapEntryNodeBuilder.create().withNodeIdentifier((NodeIdentifierWithPredicates) identifier);
        }

    }

    public static class LeafSetModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Optional<ModificationApplyOperation> entryStrategy;

        protected LeafSetModificationStrategy(final LeafListSchemaNode schema) {
            entryStrategy = Optional.<ModificationApplyOperation> of(new LeafSetEntryModificationStrategy(schema));
        }

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
            entryStrategy = Optional.<ModificationApplyOperation> of(new ListEntryModificationStrategy(schema));
        }

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
