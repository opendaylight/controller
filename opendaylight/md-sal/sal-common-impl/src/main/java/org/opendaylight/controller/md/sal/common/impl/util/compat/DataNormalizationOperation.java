/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jdt.annotation.Nullable;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.AnyxmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.util.EffectiveAugmentationSchema;

@Deprecated(forRemoval = true)
public abstract class DataNormalizationOperation<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;
    private final DataSchemaNode dataSchemaNode;

    @Override
    public T getIdentifier() {
        return identifier;
    }

    protected DataNormalizationOperation(final T identifier, final SchemaNode schema) {
        this.identifier = identifier;
        dataSchemaNode = schema instanceof DataSchemaNode ? (DataSchemaNode)schema : null;
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

    public abstract DataNormalizationOperation<?> getChild(PathArgument child) throws DataNormalizationException;

    public abstract DataNormalizationOperation<?> getChild(QName child) throws DataNormalizationException;

    public abstract boolean isLeaf();

    @SuppressModernizer
    public Optional<DataSchemaNode> getDataSchemaNode() {
        return Optional.fromNullable(dataSchemaNode);
    }

    private abstract static class SimpleTypeNormalization<T extends PathArgument>
            extends DataNormalizationOperation<T> {
        SimpleTypeNormalization(final T identifier, final DataSchemaNode potential) {
            super(identifier,potential);
        }

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
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }
    }

    private static final class LeafNormalization extends SimpleTypeNormalization<NodeIdentifier> {
        LeafNormalization(final LeafSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()),potential);
        }
    }

    private static final class LeafListEntryNormalization extends SimpleTypeNormalization<NodeWithValue> {
        LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null),potential);
        }

        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }

    private abstract static class CompositeNodeNormalizationOperation<T extends PathArgument>
            extends DataNormalizationOperation<T> {
        CompositeNodeNormalizationOperation(final T identifier, final DataSchemaNode schema) {
            super(identifier,schema);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }

    private abstract static class DataContainerNormalizationOperation<T extends PathArgument>
            extends CompositeNodeNormalizationOperation<T> {
        private final DataNodeContainer schema;
        private final Map<QName, DataNormalizationOperation<?>> byQName;
        private final Map<PathArgument, DataNormalizationOperation<?>> byArg;

        DataContainerNormalizationOperation(final T identifier, final DataNodeContainer schema,
                final DataSchemaNode node) {
            super(identifier,node);
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
            potential = fromLocalSchema(child);
            return register(potential);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) throws DataNormalizationException {
            DataNormalizationOperation<?> potential = byQName.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromLocalSchemaAndQName(schema, child);
            return register(potential);
        }

        private DataNormalizationOperation<?> fromLocalSchema(final PathArgument child)
                throws DataNormalizationException {
            if (child instanceof AugmentationIdentifier) {
                return fromSchemaAndQNameChecked(schema, ((AugmentationIdentifier) child).getPossibleChildNames()
                        .iterator().next());
            }
            return fromSchemaAndQNameChecked(schema, child.getNodeType());
        }

        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema2,
                final QName child) throws DataNormalizationException {
            return fromSchemaAndQNameChecked(schema2, child);
        }

        private DataNormalizationOperation<?> register(final DataNormalizationOperation<?> potential) {
            if (potential != null) {
                byArg.put(potential.getIdentifier(), potential);
                for (final QName qname : potential.getQNameIdentifiers()) {
                    byQName.put(qname, potential);
                }
            }
            return potential;
        }

        private static DataNormalizationOperation<?> fromSchemaAndQNameChecked(final DataNodeContainer schema,
                final QName child) throws DataNormalizationException {

            final DataSchemaNode result = findChildSchemaNode(schema, child);
            if (result == null) {
                throw new DataNormalizationException(String.format(
                        "Supplied QName %s is not valid according to schema %s, potential children nodes: %s", child,
                        schema,schema.getChildNodes()));
            }

            // We try to look up if this node was added by augmentation
            if (schema instanceof DataSchemaNode && result.isAugmenting()) {
                return fromAugmentation(schema, (AugmentationTarget) schema, result);
            }
            return fromDataSchemaNode(result);
        }
    }

    private static final class ListItemNormalization extends
            DataContainerNormalizationOperation<NodeIdentifierWithPredicates> {
        ListItemNormalization(final NodeIdentifierWithPredicates identifier, final ListSchemaNode schema) {
            super(identifier, schema, schema);
        }

        @Override
        @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder = Builders
                    .mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates) currentArg);
            for (final Entry<QName, Object> keyValue : ((NodeIdentifierWithPredicates) currentArg).entrySet()) {
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

    private static final class UnkeyedListItemNormalization
            extends DataContainerNormalizationOperation<NodeIdentifier> {
        UnkeyedListItemNormalization(final ListSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema,schema);
        }

        @Override
        @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.unkeyedListEntryBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }
    }

    private static final class ContainerNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {
        ContainerNormalization(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()),schema, schema);
        }

        @Override
        @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.containerBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }
    }

    private abstract static class MixinNormalizationOp<T extends PathArgument>
            extends CompositeNodeNormalizationOperation<T> {

        MixinNormalizationOp(final T identifier, final DataSchemaNode schema) {
            super(identifier,schema);
        }

        @Override
        public final boolean isMixin() {
            return true;
        }
    }

    private static final class OrderedLeafListMixinNormalization extends UnorderedLeafListMixinNormalization {
        OrderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(potential);
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.orderedLeafSetBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class UnorderedLeafListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final DataNormalizationOperation<?> innerOp;

        UnorderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()),potential);
            innerOp = new LeafListEntryNormalization(potential);
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

    private static final class AugmentationNormalization
            extends DataContainerNormalizationOperation<AugmentationIdentifier> {

        AugmentationNormalization(final AugmentationSchemaNode augmentation, final DataNodeContainer schema) {
            super(augmentationIdentifierFrom(augmentation), augmentationProxy(augmentation,schema),null);
        }

        private static DataNodeContainer augmentationProxy(final AugmentationSchemaNode augmentation,
                final DataNodeContainer schema) {
            final Set<DataSchemaNode> children = new HashSet<>();
            for (final DataSchemaNode augNode : augmentation.getChildNodes()) {
                children.add(schema.getDataChildByName(augNode.getQName()));
            }
            return new EffectiveAugmentationSchema(augmentation, children);
        }

        @Override
        public boolean isMixin() {
            return true;
        }

        @Override
        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema,
                final QName child) {
            final DataSchemaNode result = findChildSchemaNode(schema, child);
            if (result == null) {
                return null;
            }

            // We try to look up if this node was added by augmentation
            if (schema instanceof DataSchemaNode && result.isAugmenting()) {
                return fromAugmentation(schema, (AugmentationTarget) schema, result);
            }
            return fromDataSchemaNode(result);
        }

        @Override
        protected Set<QName> getQNameIdentifiers() {
            return getIdentifier().getPossibleChildNames();
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.augmentationBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class UnorderedMapMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {
        private final ListItemNormalization innerNode;

        UnorderedMapMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()),list);
            this.innerNode = new ListItemNormalization(NodeIdentifierWithPredicates.of(list.getQName(),
                    Collections.<QName, Object>emptyMap()), list);
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

    private static class UnkeyedListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {
        private final UnkeyedListItemNormalization innerNode;

        UnkeyedListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()),list);
            this.innerNode = new UnkeyedListItemNormalization(list);
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.unkeyedListBuilder().withNodeIdentifier(getIdentifier()).build();
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

    private static final class OrderedMapMixinNormalization extends UnorderedMapMixinNormalization {
        OrderedMapMixinNormalization(final ListSchemaNode list) {
            super(list);
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.orderedMapBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class ChoiceNodeNormalization extends MixinNormalizationOp<NodeIdentifier> {
        private final ImmutableMap<QName, DataNormalizationOperation<?>> byQName;
        private final ImmutableMap<PathArgument, DataNormalizationOperation<?>> byArg;

        ChoiceNodeNormalization(final ChoiceSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()),schema);
            final ImmutableMap.Builder<QName, DataNormalizationOperation<?>> byQNameBuilder = ImmutableMap.builder();
            final ImmutableMap.Builder<PathArgument, DataNormalizationOperation<?>> byArgBuilder =
                    ImmutableMap.builder();

            for (final CaseSchemaNode caze : schema.getCases().values()) {
                for (final DataSchemaNode cazeChild : caze.getChildNodes()) {
                    final DataNormalizationOperation<?> childOp = fromDataSchemaNode(cazeChild);
                    byArgBuilder.put(childOp.getIdentifier(), childOp);
                    for (final QName qname : childOp.getQNameIdentifiers()) {
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
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class AnyxmlNormalization extends DataNormalizationOperation<NodeIdentifier> {
        AnyxmlNormalization(final AnyxmlSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return null;
        }
    }

    private static @Nullable DataSchemaNode findChildSchemaNode(final DataNodeContainer parent, final QName child) {
        final DataSchemaNode potential = parent.getDataChildByName(child);
        return potential != null ? potential : findChoice(parent, child);
    }

    private static @Nullable ChoiceSchemaNode findChoice(final DataNodeContainer parent, final QName child) {
        for (final ChoiceSchemaNode choice : Iterables.filter(parent.getChildNodes(), ChoiceSchemaNode.class)) {
            for (final CaseSchemaNode caze : choice.getCases().values()) {
                if (findChildSchemaNode(caze, child) != null) {
                    return choice;
                }
            }
        }
        return null;
    }

    public static AugmentationIdentifier augmentationIdentifierFrom(final AugmentationSchemaNode augmentation) {
        final ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (final DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(potentialChildren.build());
    }

    /**
     * Returns a DataNormalizationOperation for provided child node.
     *
     * <p>
     * If supplied child is added by Augmentation this operation returns
     * a DataNormalizationOperation for augmentation,
     * otherwise returns a DataNormalizationOperation for child as
     * call for {@link #fromDataSchemaNode(DataSchemaNode)}.
     */
    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private static DataNormalizationOperation<?> fromAugmentation(final DataNodeContainer parent,
            final AugmentationTarget parentAug, final DataSchemaNode child) {
        AugmentationSchemaNode augmentation = null;
        for (final AugmentationSchemaNode aug : parentAug.getAvailableAugmentations()) {
            final DataSchemaNode potential = aug.getDataChildByName(child.getQName());
            if (potential != null) {
                augmentation = aug;
                break;
            }

        }
        if (augmentation != null) {
            return new AugmentationNormalization(augmentation, parent);
        } else {
            return fromDataSchemaNode(child);
        }
    }

    public static DataNormalizationOperation<?> fromDataSchemaNode(final DataSchemaNode potential) {
        if (potential instanceof ContainerSchemaNode) {
            return new ContainerNormalization((ContainerSchemaNode) potential);
        } else if (potential instanceof ListSchemaNode) {

            return fromListSchemaNode((ListSchemaNode) potential);
        } else if (potential instanceof LeafSchemaNode) {
            return new LeafNormalization((LeafSchemaNode) potential);
        } else if (potential instanceof ChoiceSchemaNode) {
            return new ChoiceNodeNormalization((ChoiceSchemaNode) potential);
        } else if (potential instanceof LeafListSchemaNode) {
            return fromLeafListSchemaNode((LeafListSchemaNode) potential);
        } else if (potential instanceof AnyxmlSchemaNode) {
            return new AnyxmlNormalization((AnyxmlSchemaNode) potential);
        }
        return null;
    }

    private static DataNormalizationOperation<?> fromListSchemaNode(final ListSchemaNode potential) {
        final List<QName> keyDefinition = potential.getKeyDefinition();
        if (keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListMixinNormalization(potential);
        }
        if (potential.isUserOrdered()) {
            return new OrderedMapMixinNormalization(potential);
        }
        return new UnorderedMapMixinNormalization(potential);
    }

    private static DataNormalizationOperation<?> fromLeafListSchemaNode(final LeafListSchemaNode potential) {
        if (potential.isUserOrdered()) {
            return new OrderedLeafListMixinNormalization(potential);
        }
        return new UnorderedLeafListMixinNormalization(potential);
    }


    public static DataNormalizationOperation<?> from(final SchemaContext ctx) {
        return new ContainerNormalization(ctx);
    }

    public abstract NormalizedNode<?, ?> createDefault(PathArgument currentArg);
}
