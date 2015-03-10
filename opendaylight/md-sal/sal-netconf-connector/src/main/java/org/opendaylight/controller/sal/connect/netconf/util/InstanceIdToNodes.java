/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.AttributesBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
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

/**
 * Transforms an instance of yang instance identifier to a filter like structure in normalized node format. Can be also used to nest the edit-config rpc content.
 * For each argument of the id, a specific normalized node is created to ensure schema context conformance.
 */
public abstract class InstanceIdToNodes<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;

    @Override
    public T getIdentifier() {
        return identifier;
    }

    protected InstanceIdToNodes(final T identifier) {
        this.identifier = identifier;
    }

    abstract InstanceIdToNodes<?> getChild(final PathArgument child) throws DataNormalizationException;

    public abstract NormalizedNode<?, ?> create(YangInstanceIdentifier legacyData, Optional<NormalizedNode<?, ?>> deepestChild, Optional<ModifyAction> operation);

    private static abstract class SimpleTypeNormalization<T extends PathArgument> extends InstanceIdToNodes<T> {

        protected SimpleTypeNormalization(final T identifier) {
            super(identifier);
        }

        @Override
        public NormalizedNode<?, ?> create(final YangInstanceIdentifier id, final Optional<NormalizedNode<?, ?>> deepestChild, final Optional<ModifyAction> operation) {
            checkNotNull(id);
            final PathArgument pathArgument = Iterables.get(id.getPathArguments(), 0);
            final NormalizedNodeAttrBuilder<? extends PathArgument, Object, ? extends NormalizedNode<? extends PathArgument, Object>> builder = getBuilder(pathArgument);

            if(deepestChild.isPresent()) {
                builder.withValue(deepestChild.get().getValue());
            }

            addModifyOpIfPresent(operation, builder);
            return builder.build();
        }

        protected abstract NormalizedNodeAttrBuilder<? extends PathArgument, Object, ? extends NormalizedNode<? extends PathArgument, Object>> getBuilder(PathArgument node);

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) {
            return null;
        }
    }


    public void addModifyOpIfPresent(final Optional<ModifyAction> operation, final AttributesBuilder<?> builder) {
        if(operation.isPresent()) {
            builder.withAttributes(Collections.singletonMap(NetconfMessageTransformUtil.NETCONF_OPERATION_QNAME, NetconfMessageTransformUtil.modifyOperationToXmlString(operation.get())));
        }
    }

    private static final class LeafNormalization extends SimpleTypeNormalization<NodeIdentifier> {

        protected LeafNormalization(final LeafSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()));
        }

        @Override
        protected NormalizedNodeAttrBuilder<NodeIdentifier, Object, LeafNode<Object>> getBuilder(final PathArgument node) {
            return Builders.leafBuilder().withNodeIdentifier(getIdentifier());
        }
    }

    private static final class LeafListEntryNormalization extends SimpleTypeNormalization<NodeWithValue> {

        public LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null));
        }

        @Override
        protected NormalizedNodeAttrBuilder<NodeWithValue, Object, LeafSetEntryNode<Object>> getBuilder(final PathArgument node) {
            Preconditions.checkArgument(node instanceof NodeWithValue);
            return Builders.leafSetEntryBuilder().withNodeIdentifier((NodeWithValue) node).withValue(((NodeWithValue) node).getValue());
        }

    }

    private static abstract class CompositeNodeNormalizationOperation<T extends PathArgument> extends
            InstanceIdToNodes<T> {

        protected CompositeNodeNormalizationOperation(final T identifier) {
            super(identifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final NormalizedNode<?, ?> create(final YangInstanceIdentifier id, final Optional<NormalizedNode<?, ?>> lastChild, final Optional<ModifyAction> operation) {
            checkNotNull(id);
            final Iterator<PathArgument> iterator = id.getPathArguments().iterator();
            final PathArgument legacyData = iterator.next();

            if (!isMixin(this) && getIdentifier().getNodeType() != null) {
                checkArgument(getIdentifier().getNodeType().equals(legacyData.getNodeType()),
                        "Node QName must be %s was %s", getIdentifier().getNodeType(), legacyData.getNodeType());
            }
            final NormalizedNodeContainerBuilder builder = createBuilder(legacyData);

            if (iterator.hasNext()) {
                final PathArgument childPath = iterator.next();
                final InstanceIdToNodes childOp = getChildOperation(childPath);

                final YangInstanceIdentifier childId = YangInstanceIdentifier.create(Iterables.skip(id.getPathArguments(), 1));
                builder.addChild(childOp.create(childId, lastChild, operation));
            } else {
                if(lastChild.isPresent()) {
                    builder.withValue(Lists.newArrayList((Collection<?>) lastChild.get().getValue()));
                }
                if(operation.isPresent()) {
                    Preconditions.checkArgument(builder instanceof AttributesBuilder<?>);
                    addModifyOpIfPresent(operation, ((AttributesBuilder<?>) builder));
                }
            }

            return builder.build();
        }

        private InstanceIdToNodes getChildOperation(final PathArgument childPath) {
            final InstanceIdToNodes childOp;
            try {
                childOp = getChild(childPath);
            } catch (final DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Failed to process child node %s", childPath), e);
            }
            checkArgument(childOp != null, "Node %s is not allowed inside %s", childPath, getIdentifier());
            return childOp;
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode);
    }

    static boolean isMixin(final InstanceIdToNodes<?> op) {
        return op instanceof MixinNormalizationOp;
    }

    private static abstract class DataContainerNormalizationOperation<T extends PathArgument> extends
            CompositeNodeNormalizationOperation<T> {

        private final DataNodeContainer schema;
        private final Map<PathArgument, InstanceIdToNodes<?>> byArg;

        protected DataContainerNormalizationOperation(final T identifier, final DataNodeContainer schema) {
            super(identifier);
            this.schema = schema;
            this.byArg = new ConcurrentHashMap<>();
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) throws DataNormalizationException {
            InstanceIdToNodes<?> potential = byArg.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromLocalSchema(child);
            return register(potential);
        }

        private InstanceIdToNodes<?> fromLocalSchema(final PathArgument child) throws DataNormalizationException {
            if (child instanceof AugmentationIdentifier) {
                return fromSchemaAndQNameChecked(schema, ((AugmentationIdentifier) child).getPossibleChildNames()
                        .iterator().next());
            }
            return fromSchemaAndQNameChecked(schema, child.getNodeType());
        }

        private InstanceIdToNodes<?> register(final InstanceIdToNodes<?> potential) {
            if (potential != null) {
                byArg.put(potential.getIdentifier(), potential);
            }
            return potential;
        }
    }

    private static final class ListItemNormalization extends
            DataContainerNormalizationOperation<NodeIdentifierWithPredicates> {

        protected ListItemNormalization(final NodeIdentifierWithPredicates identifier, final ListSchemaNode schema) {
            super(identifier, schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument currentArg) {
            final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder = Builders
                    .mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates) currentArg);
            for (final Entry<QName, Object> keyValue : ((NodeIdentifierWithPredicates) currentArg).getKeyValues().entrySet()) {
                builder.addChild(Builders.leafBuilder()
                        //
                        .withNodeIdentifier(new NodeIdentifier(keyValue.getKey())).withValue(keyValue.getValue())
                        .build());
            }
            return builder;
        }

    }

    private static final class UnkeyedListItemNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected UnkeyedListItemNormalization(final ListSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.unkeyedListEntryBuilder().withNodeIdentifier(getIdentifier());
        }

    }

    private static final class ContainerTransformation extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected ContainerTransformation(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.containerBuilder().withNodeIdentifier(getIdentifier());
        }
    }

    /**
     * Marker interface for Mixin nodes normalization operations
     */
    private interface MixinNormalizationOp {}


    private static final class OrderedLeafListMixinNormalization extends UnorderedLeafListMixinNormalization {


        public OrderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.orderedLeafSetBuilder().withNodeIdentifier(getIdentifier());
        }
    }

    private static class UnorderedLeafListMixinNormalization extends CompositeNodeNormalizationOperation<NodeIdentifier> implements MixinNormalizationOp {

        private final InstanceIdToNodes<?> innerOp;

        public UnorderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()));
            innerOp = new LeafListEntryNormalization(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.leafSetBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) {
            if (child instanceof NodeWithValue) {
                return innerOp;
            }
            return null;
        }
    }

    private static final class AugmentationNormalization extends DataContainerNormalizationOperation<AugmentationIdentifier> implements MixinNormalizationOp {

        public AugmentationNormalization(final AugmentationSchema augmentation, final DataNodeContainer schema) {
            //super();
            super(augmentationIdentifierFrom(augmentation), augmentationProxy(augmentation, schema));
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.augmentationBuilder().withNodeIdentifier(getIdentifier());
        }
    }

    private static class UnorderedMapMixinNormalization extends CompositeNodeNormalizationOperation<NodeIdentifier> implements MixinNormalizationOp {

        private final ListItemNormalization innerNode;

        public UnorderedMapMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()));
            this.innerNode = new ListItemNormalization(new NodeIdentifierWithPredicates(list.getQName(),
                    Collections.<QName, Object>emptyMap()), list);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.mapBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }
    }

    private static class UnkeyedListMixinNormalization extends CompositeNodeNormalizationOperation<NodeIdentifier> implements MixinNormalizationOp {

        private final UnkeyedListItemNormalization innerNode;

        public UnkeyedListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()));
            this.innerNode = new UnkeyedListItemNormalization(list);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.unkeyedListBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }

    }

    private static final class OrderedMapMixinNormalization extends UnorderedMapMixinNormalization {

        public OrderedMapMixinNormalization(final ListSchemaNode list) {
            super(list);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.orderedMapBuilder().withNodeIdentifier(getIdentifier());
        }

    }

    private static class ChoiceNodeNormalization extends CompositeNodeNormalizationOperation<NodeIdentifier> implements MixinNormalizationOp {

        private final ImmutableMap<PathArgument, InstanceIdToNodes<?>> byArg;

        protected ChoiceNodeNormalization(final org.opendaylight.yangtools.yang.model.api.ChoiceNode schema) {
            super(new NodeIdentifier(schema.getQName()));
            final ImmutableMap.Builder<PathArgument, InstanceIdToNodes<?>> byArgBuilder = ImmutableMap.builder();

            for (final ChoiceCaseNode caze : schema.getCases()) {
                for (final DataSchemaNode cazeChild : caze.getChildNodes()) {
                    final InstanceIdToNodes<?> childOp = fromDataSchemaNode(cazeChild);
                    byArgBuilder.put(childOp.getIdentifier(), childOp);
                }
            }
            byArg = byArgBuilder.build();
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) {
            return byArg.get(child);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final PathArgument compositeNode) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier());
        }
    }

    private static class AnyXmlNormalization extends InstanceIdToNodes<NodeIdentifier> {

        protected AnyXmlNormalization(final AnyXmlSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()));
        }

        @Override
        public InstanceIdToNodes<?> getChild(final PathArgument child) throws DataNormalizationException {
            return null;
        }

        @Override
        public NormalizedNode<?, ?> create(final YangInstanceIdentifier legacyData, final Optional<NormalizedNode<?, ?>> deepestChild, final Optional<ModifyAction> operation) {
            if(deepestChild.isPresent()) {
                Preconditions.checkState(deepestChild instanceof AnyXmlNode);
                final NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> anyXmlBuilder =
                        Builders.anyXmlBuilder().withNodeIdentifier(getIdentifier()).withValue(((AnyXmlNode) deepestChild).getValue());
                addModifyOpIfPresent(operation, anyXmlBuilder);
                return anyXmlBuilder.build();
            }

            final NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> builder =
                    Builders.anyXmlBuilder().withNodeIdentifier(getIdentifier());
            addModifyOpIfPresent(operation, builder);
            return builder.build();
        }

    }

    private static Optional<DataSchemaNode> findChildSchemaNode(final DataNodeContainer parent, final QName child) {
        DataSchemaNode potential = parent.getDataChildByName(child);
        if (potential == null) {
            final Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices = FluentIterable.from(
                    parent.getChildNodes()).filter(org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }
        return Optional.fromNullable(potential);
    }

    private static InstanceIdToNodes<?> fromSchemaAndQNameChecked(final DataNodeContainer schema, final QName child) throws DataNormalizationException {
        final Optional<DataSchemaNode> potential = findChildSchemaNode(schema, child);
        if (!potential.isPresent()) {
            throw new DataNormalizationException(String.format("Supplied QName %s is not valid according to schema %s, potential children nodes: %s", child, schema, schema.getChildNodes()));
        }

        final DataSchemaNode result = potential.get();
        // We try to look up if this node was added by augmentation
        if ((schema instanceof DataSchemaNode) && result.isAugmenting()) {
            return fromAugmentation(schema, (AugmentationTarget) schema, result);
        }
        return fromDataSchemaNode(result);
    }

    private static org.opendaylight.yangtools.yang.model.api.ChoiceNode findChoice(
            final Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices, final QName child) {
        org.opendaylight.yangtools.yang.model.api.ChoiceNode foundChoice = null;
        choiceLoop:
        for (final org.opendaylight.yangtools.yang.model.api.ChoiceNode choice : choices) {
            for (final ChoiceCaseNode caze : choice.getCases()) {
                if (findChildSchemaNode(caze, child).isPresent()) {
                    foundChoice = choice;
                    break choiceLoop;
                }
            }
        }
        return foundChoice;
    }

    private static AugmentationIdentifier augmentationIdentifierFrom(final AugmentationSchema augmentation) {
        final ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (final DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(potentialChildren.build());
    }

    private static DataNodeContainer augmentationProxy(final AugmentationSchema augmentation, final DataNodeContainer schema) {
        final Set<DataSchemaNode> children = new HashSet<>();
        for (final DataSchemaNode augNode : augmentation.getChildNodes()) {
            children.add(schema.getDataChildByName(augNode.getQName()));
        }
        return new NodeContainerProxy(null, children);
    }

    /**
     * Returns a SchemaPathUtil for provided child node
     * <p/>
     * If supplied child is added by Augmentation this operation returns
     * a SchemaPathUtil for augmentation,
     * otherwise returns a SchemaPathUtil for child as
     * call for {@link #fromDataSchemaNode(org.opendaylight.yangtools.yang.model.api.DataSchemaNode)}.
     */
    private static InstanceIdToNodes<?> fromAugmentation(final DataNodeContainer parent,
                                                          final AugmentationTarget parentAug, final DataSchemaNode child) {
        AugmentationSchema augmentation = null;
        for (final AugmentationSchema aug : parentAug.getAvailableAugmentations()) {
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

    private static InstanceIdToNodes<?> fromDataSchemaNode(final DataSchemaNode potential) {
        if (potential instanceof ContainerSchemaNode) {
            return new ContainerTransformation((ContainerSchemaNode) potential);
        } else if (potential instanceof ListSchemaNode) {
            return fromListSchemaNode((ListSchemaNode) potential);
        } else if (potential instanceof LeafSchemaNode) {
            return new LeafNormalization((LeafSchemaNode) potential);
        } else if (potential instanceof org.opendaylight.yangtools.yang.model.api.ChoiceNode) {
            return new ChoiceNodeNormalization((org.opendaylight.yangtools.yang.model.api.ChoiceNode) potential);
        } else if (potential instanceof LeafListSchemaNode) {
            return fromLeafListSchemaNode((LeafListSchemaNode) potential);
        } else if (potential instanceof AnyXmlSchemaNode) {
            return new AnyXmlNormalization((AnyXmlSchemaNode) potential);
        }
        return null;
    }

    private static InstanceIdToNodes<?> fromListSchemaNode(final ListSchemaNode potential) {
        final List<QName> keyDefinition = potential.getKeyDefinition();
        if (keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListMixinNormalization(potential);
        }
        if (potential.isUserOrdered()) {
            return new OrderedMapMixinNormalization(potential);
        }
        return new UnorderedMapMixinNormalization(potential);
    }

    private static InstanceIdToNodes<?> fromLeafListSchemaNode(final LeafListSchemaNode potential) {
        if (potential.isUserOrdered()) {
            return new OrderedLeafListMixinNormalization(potential);
        }
        return new UnorderedLeafListMixinNormalization(potential);
    }

    public static NormalizedNode<?, ?> serialize(final SchemaContext ctx, final YangInstanceIdentifier id) {
        return serialize(ctx, id, Optional.<NormalizedNode<?, ?>>absent(), Optional.<ModifyAction>absent());
    }

    public static NormalizedNode<?, ?> serialize(final SchemaContext ctx, final YangInstanceIdentifier id, final NormalizedNode<?, ?> deepestElement) {
        return serialize(ctx, id, Optional.<NormalizedNode<?, ?>>of(deepestElement), Optional.<ModifyAction>absent());
    }

    public static NormalizedNode<?, ?> serialize(final SchemaContext ctx, final YangInstanceIdentifier id, final Optional<NormalizedNode<?, ?>> deepestElement, final Optional<ModifyAction> operation) {
        Preconditions.checkNotNull(ctx);
        Preconditions.checkNotNull(id);
        final PathArgument topLevelElement = id.getPathArguments().iterator().next();
        final DataSchemaNode dataChildByName = ctx.getDataChildByName(topLevelElement.getNodeType());
        Preconditions.checkNotNull(dataChildByName, "Cannot find %s node in schema context. Instance identifier has to start from root", topLevelElement);
        try {
            final InstanceIdToNodes<?> instanceIdToNodes = fromSchemaAndQNameChecked(ctx, topLevelElement.getNodeType());
            return instanceIdToNodes.create(id, deepestElement, operation);
        } catch (final DataNormalizationException e) {
            throw new IllegalArgumentException("Unable to serialize: " + id, e);
        }
    }
}
