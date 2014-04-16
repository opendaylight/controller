/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public abstract class DataNormalizationOperation<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;

    @Override
    public T getIdentifier() {
        return identifier;
    };

    protected DataNormalizationOperation(final T identifier) {
        super();
        this.identifier = identifier;
    }

    public boolean isMixin() {
        return false;
    }


    public boolean isKeyedEntry() {
        return false;
    }

    protected Set<QName> getQNameIdentifiers() {
        return Collections.singleton(identifier.getNodeType());
    }

    public abstract DataNormalizationOperation<?> getChild(final PathArgument child) throws DataNormalizationException;

    public abstract DataNormalizationOperation<?> getChild(QName child) throws DataNormalizationException;

    public abstract NormalizedNode<?, ?> normalize(Node<?> legacyData);

    private static abstract class SimpleTypeNormalization<T extends PathArgument> extends DataNormalizationOperation<T> {

        protected SimpleTypeNormalization(final T identifier) {
            super(identifier);
        }

        @Override
        public NormalizedNode<?, ?> normalize(final Node<?> legacyData) {
            checkArgument(legacyData != null);
            checkArgument(legacyData instanceof SimpleNode<?>);
            return normalizeImpl((SimpleNode<?>) legacyData);
        }

        protected abstract NormalizedNode<?, ?> normalizeImpl(SimpleNode<?> node);

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            return null;
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private static final class LeafNormalization extends SimpleTypeNormalization<NodeIdentifier> {

        protected LeafNormalization(final NodeIdentifier identifier) {
            super(identifier);
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final SimpleNode<?> node) {
            return ImmutableNodes.leafNode(node.getNodeType(), node.getValue());
        }

    }

    private static final class LeafListEntryNormalization extends SimpleTypeNormalization<NodeWithValue> {

        public LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null));
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final SimpleNode<?> node) {
            NodeWithValue nodeId = new NodeWithValue(node.getNodeType(), node.getValue());
            return Builders.leafSetEntryBuilder().withNodeIdentifier(nodeId).withValue(node.getValue()).build();
        }


        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }

    private static abstract class CompositeNodeNormalizationOpertation<T extends PathArgument> extends
            DataNormalizationOperation<T> {

        protected CompositeNodeNormalizationOpertation(final T identifier) {
            super(identifier);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public final NormalizedNodeContainer<?, ?, ?> normalize(final Node<?> legacyData) {
            checkArgument(legacyData != null);
            if (!isMixin() && getIdentifier().getNodeType() != null) {
                checkArgument(getIdentifier().getNodeType().equals(legacyData.getNodeType()),
                        "Node QName must be %s was %s", getIdentifier().getNodeType(), legacyData.getNodeType());
            }
            checkArgument(legacyData instanceof CompositeNode, "Node %s should be composite", legacyData);
            CompositeNode compositeNode = (CompositeNode) legacyData;
            NormalizedNodeContainerBuilder builder = createBuilder(compositeNode);

            Set<DataNormalizationOperation<?>> usedMixins = new HashSet<>();
            for (Node<?> childLegacy : compositeNode.getValue()) {
                final DataNormalizationOperation childOp;

                try {
                    childOp = getChild(childLegacy.getNodeType());
                } catch (DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Failed to normalize data %s", compositeNode.getValue()), e);
                }

                // We skip unknown nodes if this node is mixin since
                // it's nodes and parent nodes are interleaved
                if (childOp == null && isMixin()) {
                    continue;
                }

                checkArgument(childOp != null, "Node %s is not allowed inside %s", childLegacy.getNodeType(),
                        getIdentifier());
                if (childOp.isMixin()) {
                    if (usedMixins.contains(childOp)) {
                        // We already run / processed that mixin, so to avoid
                        // duplicity we are skipping next nodes.
                        continue;
                    }
                    builder.addChild(childOp.normalize(compositeNode));
                    usedMixins.add(childOp);
                } else {
                    builder.addChild(childOp.normalize(childLegacy));
                }
            }
            return (NormalizedNodeContainer<?, ?, ?>) builder.build();
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode);

    }

    private static abstract class DataContainerNormalizationOperation<T extends PathArgument> extends
            CompositeNodeNormalizationOpertation<T> {

        private final DataNodeContainer schema;
        private final Map<QName, DataNormalizationOperation<?>> byQName;
        private final Map<PathArgument, DataNormalizationOperation<?>> byArg;

        protected DataContainerNormalizationOperation(final T identifier, final DataNodeContainer schema) {
            super(identifier);
            this.schema = schema;
            this.byArg = new ConcurrentHashMap<>();
            this.byQName = new ConcurrentHashMap<>();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) throws DataNormalizationException {
            DataNormalizationOperation<?> potential = byArg.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromSchema(schema, child);
            return register(potential);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) throws DataNormalizationException {
            DataNormalizationOperation<?> potential = byQName.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromSchemaAndPathArgument(schema, child);
            return register(potential);
        }

        private DataNormalizationOperation<?> register(final DataNormalizationOperation<?> potential) {
            if (potential != null) {
                byArg.put(potential.getIdentifier(), potential);
                for (QName qName : potential.getQNameIdentifiers()) {
                    byQName.put(qName, potential);
                }
            }
            return potential;
        }

    }

    private static final class ListItemNormalization extends
            DataContainerNormalizationOperation<NodeIdentifierWithPredicates> {

        private final List<QName> keyDefinition;

        protected ListItemNormalization(final NodeIdentifierWithPredicates identifier, final ListSchemaNode schema) {
            super(identifier, schema);
            keyDefinition = schema.getKeyDefinition();
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            ImmutableMap.Builder<QName, Object> keys = ImmutableMap.builder();
            for (QName key : keyDefinition) {

                SimpleNode<?> valueNode = checkNotNull(compositeNode.getFirstSimpleByName(key),
                        "List node %s MUST contain leaf %s with value.", getIdentifier().getNodeType(), key);
                keys.put(key, valueNode.getValue());
            }

            return Builders.mapEntryBuilder().withNodeIdentifier(
                    new NodeIdentifierWithPredicates(getIdentifier().getNodeType(), keys.build()));
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder = Builders
                    .mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates) currentArg);
            for (Entry<QName, Object> keyValue : ((NodeIdentifierWithPredicates) currentArg).getKeyValues().entrySet()) {
                builder.addChild(Builders.leafBuilder()
                        //
                        .withNodeIdentifier(new NodeIdentifier(keyValue.getKey())).withValue(keyValue.getValue())
                        .build());
            }
            return builder.build();
        }


        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }

    private static final class ContainerNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected ContainerNormalization(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.containerBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.containerBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }

    }

    private static abstract class MixinNormalizationOp<T extends PathArgument> extends
            CompositeNodeNormalizationOpertation<T> {

        protected MixinNormalizationOp(final T identifier) {
            super(identifier);
        }

        @Override
        public final boolean isMixin() {
            return true;
        }

    }

    private static final class LeafListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final DataNormalizationOperation<?> innerOp;

        public LeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()));
            innerOp = new LeafListEntryNormalization(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.leafSetBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.leafSetBuilder().withNodeIdentifier(getIdentifier()).build();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            if (child instanceof NodeWithValue) {
                return innerOp;
            }
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerOp;
            }
            return null;
        }
    }

    private static final class AugmentationNormalization extends MixinNormalizationOp<AugmentationIdentifier> {

        private final Map<QName, DataNormalizationOperation<?>> byQName;
        private final Map<PathArgument, DataNormalizationOperation<?>> byArg;

        public AugmentationNormalization(final AugmentationSchema augmentation, final DataNodeContainer schema) {
            super(augmentationIdentifierFrom(augmentation));

            ImmutableMap.Builder<QName, DataNormalizationOperation<?>> byQNameBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<PathArgument, DataNormalizationOperation<?>> byArgBuilder = ImmutableMap.builder();

            for (DataSchemaNode augNode : augmentation.getChildNodes()) {
                DataSchemaNode resolvedNode = schema.getDataChildByName(augNode.getQName());
                DataNormalizationOperation<?> resolvedOp = fromDataSchemaNode(resolvedNode);
                byArgBuilder.put(resolvedOp.getIdentifier(), resolvedOp);
                for (QName resQName : resolvedOp.getQNameIdentifiers()) {
                    byQNameBuilder.put(resQName, resolvedOp);
                }
            }
            byQName = byQNameBuilder.build();
            byArg = byArgBuilder.build();

        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            return byArg.get(child);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            return byQName.get(child);
        }

        @Override
        protected Set<QName> getQNameIdentifiers() {
            return getIdentifier().getPossibleChildNames();
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.augmentationBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.augmentationBuilder().withNodeIdentifier(getIdentifier()).build();
        }

    }

    private static final class ListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ListItemNormalization innerNode;

        public ListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()));
            this.innerNode = new ListItemNormalization(new NodeIdentifierWithPredicates(list.getQName(),
                    Collections.<QName, Object> emptyMap()), list);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.mapBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.mapBuilder().withNodeIdentifier(getIdentifier()).build();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerNode;
            }
            return null;
        }

    }

    private static class ChoiceNodeNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ImmutableMap<QName, DataNormalizationOperation<?>> byQName;
        private final ImmutableMap<PathArgument, DataNormalizationOperation<?>> byArg;

        protected ChoiceNodeNormalization(final org.opendaylight.yangtools.yang.model.api.ChoiceNode schema) {
            super(new NodeIdentifier(schema.getQName()));
            ImmutableMap.Builder<QName, DataNormalizationOperation<?>> byQNameBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<PathArgument, DataNormalizationOperation<?>> byArgBuilder = ImmutableMap.builder();

            for (ChoiceCaseNode caze : schema.getCases()) {
                for (DataSchemaNode cazeChild : caze.getChildNodes()) {
                    DataNormalizationOperation<?> childOp = fromDataSchemaNode(cazeChild);
                    byArgBuilder.put(childOp.getIdentifier(), childOp);
                    for (QName qname : childOp.getQNameIdentifiers()) {
                        byQNameBuilder.put(qname, childOp);
                    }
                }
            }
            byQName = byQNameBuilder.build();
            byArg = byArgBuilder.build();
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            return byArg.get(child);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            return byQName.get(child);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static DataNormalizationOperation<?> fromSchemaAndPathArgument(final DataNodeContainer schema,
            final QName child) throws DataNormalizationException {
        DataSchemaNode potential = schema.getDataChildByName(child);
        if (potential == null) {
            Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices = FluentIterable.from(
                    schema.getChildNodes()).filter(org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }

        if (potential == null) {
            throw new DataNormalizationException(String.format("Supplied QName %s is not valid according to schema %s, potential children nodes: %s", child, schema,schema.getChildNodes()));
        }

        if ((schema instanceof DataSchemaNode) && !((DataSchemaNode) schema).isAugmenting() && potential.isAugmenting()) {
            return fromAugmentation(schema, (AugmentationTarget) schema, potential);
        }
        return fromDataSchemaNode(potential);
    }

    private static org.opendaylight.yangtools.yang.model.api.ChoiceNode findChoice(
            final Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices, final QName child) {
        org.opendaylight.yangtools.yang.model.api.ChoiceNode foundChoice = null;
        choiceLoop: for (org.opendaylight.yangtools.yang.model.api.ChoiceNode choice : choices) {
            for (ChoiceCaseNode caze : choice.getCases()) {
                if (caze.getDataChildByName(child) != null) {
                    foundChoice = choice;
                    break choiceLoop;
                }
            }
        }
        return foundChoice;
    }

    public static AugmentationIdentifier augmentationIdentifierFrom(final AugmentationSchema augmentation) {
        ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(null, potentialChildren.build());
    }

    private static AugmentationNormalization fromAugmentation(final DataNodeContainer schema,
            final AugmentationTarget augments, final DataSchemaNode potential) {
        AugmentationSchema augmentation = null;
        for (AugmentationSchema aug : augments.getAvailableAugmentations()) {
            DataSchemaNode child = aug.getDataChildByName(potential.getQName());
            if (child != null) {
                augmentation = aug;
                break;
            }

        }
        if (augmentation != null) {
            return new AugmentationNormalization(augmentation, schema);
        } else {
            return null;
        }
    }

    private static DataNormalizationOperation<?> fromSchema(final DataNodeContainer schema, final PathArgument child) throws DataNormalizationException {
        if (child instanceof AugmentationIdentifier) {
            return fromSchemaAndPathArgument(schema, ((AugmentationIdentifier) child).getPossibleChildNames()
                    .iterator().next());
        }
        return fromSchemaAndPathArgument(schema, child.getNodeType());
    }

    public static DataNormalizationOperation<?> fromDataSchemaNode(final DataSchemaNode potential) {
        if (potential instanceof ContainerSchemaNode) {
            return new ContainerNormalization((ContainerSchemaNode) potential);
        } else if (potential instanceof ListSchemaNode) {
            return new ListMixinNormalization((ListSchemaNode) potential);
        } else if (potential instanceof LeafSchemaNode) {
            return new LeafNormalization(new NodeIdentifier(potential.getQName()));
        } else if (potential instanceof org.opendaylight.yangtools.yang.model.api.ChoiceNode) {
            return new ChoiceNodeNormalization((org.opendaylight.yangtools.yang.model.api.ChoiceNode) potential);
        } else if (potential instanceof LeafListSchemaNode) {
            return new LeafListMixinNormalization((LeafListSchemaNode) potential);
        }
        return null;
    }

    public static DataNormalizationOperation<?> from(final SchemaContext ctx) {
        return new ContainerNormalization(ctx);
    }

    public abstract NormalizedNode<?, ?> createDefault(PathArgument currentArg);
}
