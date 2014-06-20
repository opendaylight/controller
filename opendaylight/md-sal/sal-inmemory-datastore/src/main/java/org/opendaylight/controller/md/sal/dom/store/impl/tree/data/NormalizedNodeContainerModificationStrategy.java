/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataValidationFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.DataNodeContainerModificationStrategy.ListEntryModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.ValueNodeModificationStrategy.LeafSetEntryModificationStrategy;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.MutableTreeNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNodeFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.Version;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableOrderedLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableOrderedMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

abstract class NormalizedNodeContainerModificationStrategy extends SchemaAwareApplyOperation {

    private final Class<? extends NormalizedNode<?, ?>> nodeClass;

    protected NormalizedNodeContainerModificationStrategy(final Class<? extends NormalizedNode<?, ?>> nodeClass) {
        this.nodeClass = nodeClass;
    }

    @Override
    public void verifyStructure(final ModifiedNode modification) throws IllegalArgumentException {
        if (modification.getType() == ModificationType.WRITE) {

        }
        for (ModifiedNode childModification : modification.getChildren()) {
            resolveChildOperation(childModification.getIdentifier()).verifyStructure(childModification);
        }
    }

    @Override
    protected void checkWriteApplicable(final InstanceIdentifier path, final NodeModification modification,
            final Optional<TreeNode> current) throws DataValidationFailedException {
        // FIXME: Implement proper write check for replacement of node container
        //        prerequisite is to have transaction chain available for clients
        //        otherwise this will break chained writes to same node.
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void verifyWrittenStructure(final NormalizedNode<?, ?> writtenValue) {
        checkArgument(nodeClass.isInstance(writtenValue), "Node should must be of type %s", nodeClass);
        checkArgument(writtenValue instanceof NormalizedNodeContainer);

        NormalizedNodeContainer container = (NormalizedNodeContainer) writtenValue;
        for (Object child : container.getValue()) {
            checkArgument(child instanceof NormalizedNode);

            /*
             * FIXME: fail-fast semantics:
             *
             * We can validate the data structure here, aborting the commit
             * before it ever progresses to being committed.
             */
        }
    }

    @Override
    protected TreeNode applyWrite(final ModifiedNode modification,
            final Optional<TreeNode> currentMeta, final Version version) {
        final NormalizedNode<?, ?> newValue = modification.getWrittenValue();
        final TreeNode newValueMeta = TreeNodeFactory.createTreeNode(newValue, version);

        if (Iterables.isEmpty(modification.getChildren())) {
            return newValueMeta;
        }

        /*
         * This is where things get interesting. The user has performed a write and
         * then she applied some more modifications to it. So we need to make sense
         * of that an apply the operations on top of the written value. We could have
         * done it during the write, but this operation is potentially expensive, so
         * we have left it out of the fast path.
         *
         * As it turns out, once we materialize the written data, we can share the
         * code path with the subtree change. So let's create an unsealed TreeNode
         * and run the common parts on it -- which end with the node being sealed.
         */
        final MutableTreeNode mutable = newValueMeta.mutable();
        mutable.setSubtreeVersion(version);

        @SuppressWarnings("rawtypes")
        final NormalizedNodeContainerBuilder dataBuilder = createBuilder(newValue);

        return mutateChildren(mutable, dataBuilder, version, modification.getChildren());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private TreeNode mutateChildren(final MutableTreeNode meta, final NormalizedNodeContainerBuilder data,
            final Version nodeVersion, final Iterable<ModifiedNode> modifications) {

        for (ModifiedNode mod : modifications) {
            final PathArgument id = mod.getIdentifier();
            final Optional<TreeNode> cm = meta.getChild(id);

            Optional<TreeNode> result = resolveChildOperation(id).apply(mod, cm, nodeVersion);
            if (result.isPresent()) {
                final TreeNode tn = result.get();
                meta.addChild(tn);
                data.addChild(tn.getData());
            } else {
                meta.removeChild(id);
                data.removeChild(id);
            }
        }

        meta.setData(data.build());
        return meta.seal();
    }

    @Override
    protected TreeNode applyMerge(final ModifiedNode modification, final TreeNode currentMeta,
            final Version version) {
        // For Node Containers - merge is same as subtree change - we only replace children.
        return applySubtreeChange(modification, currentMeta, version);
    }

    @Override
    public TreeNode applySubtreeChange(final ModifiedNode modification,
            final TreeNode currentMeta, final Version version) {
        final MutableTreeNode newMeta = currentMeta.mutable();
        newMeta.setSubtreeVersion(version);

        @SuppressWarnings("rawtypes")
        NormalizedNodeContainerBuilder dataBuilder = createBuilder(currentMeta.getData());

        return mutateChildren(newMeta, dataBuilder, version, modification.getChildren());
    }

    @Override
    protected void checkSubtreeModificationApplicable(final InstanceIdentifier path, final NodeModification modification,
            final Optional<TreeNode> current) throws DataValidationFailedException {
        checkConflicting(path, current.isPresent(), "Node was deleted by other transaction.");
        checkChildPreconditions(path, modification, current);
    }

    private void checkChildPreconditions(final InstanceIdentifier path, final NodeModification modification, final Optional<TreeNode> current) throws DataValidationFailedException {
        final TreeNode currentMeta = current.get();
        for (NodeModification childMod : modification.getChildren()) {
            final PathArgument childId = childMod.getIdentifier();
            final Optional<TreeNode> childMeta = currentMeta.getChild(childId);

            InstanceIdentifier childPath = path.node(childId);
            resolveChildOperation(childId).checkApplicable(childPath, childMod, childMeta);
        }
    }

    @Override
    protected void checkMergeApplicable(final InstanceIdentifier path, final NodeModification modification,
            final Optional<TreeNode> current) throws DataValidationFailedException {
        if(current.isPresent()) {
            checkChildPreconditions(path, modification,current);
        }
    }

    @SuppressWarnings("rawtypes")
    protected abstract NormalizedNodeContainerBuilder createBuilder(NormalizedNode<?, ?> original);

    public static class ChoiceModificationStrategy extends NormalizedNodeContainerModificationStrategy {

        private final Map<PathArgument, ModificationApplyOperation> childNodes;

        public ChoiceModificationStrategy(final ChoiceNode schemaNode) {
            super(org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode.class);
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
}
