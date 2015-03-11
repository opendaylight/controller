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

import javax.xml.transform.dom.DOMSource;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @deprecated This class provides compatibility between {@link CompositeNode} and {@link NormalizedNode}.
 *             Users of this class should use {@link NormalizedNode}s directly.
 */
@Deprecated
public abstract class DataNormalizationOperation<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;
    private final Optional<DataSchemaNode> dataSchemaNode;

    @Override
    public T getIdentifier() {
        return identifier;
    };

    protected DataNormalizationOperation(final T identifier, final SchemaNode schema) {
        super();
        this.identifier = identifier;
        if(schema instanceof DataSchemaNode) {
            this.dataSchemaNode = Optional.of((DataSchemaNode) schema);
        } else {
            this.dataSchemaNode = Optional.absent();
        }
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

    public abstract boolean isLeaf();

    public Optional<DataSchemaNode> getDataSchemaNode() {
        // FIXME
        return dataSchemaNode;
    }

    private static abstract class SimpleTypeNormalization<T extends PathArgument> extends DataNormalizationOperation<T> {

        protected SimpleTypeNormalization(final T identifier, final DataSchemaNode potential) {
            super(identifier,potential);
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
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

    }

    private static final class LeafNormalization extends SimpleTypeNormalization<NodeIdentifier> {

        protected LeafNormalization(final LeafSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()),potential);
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final SimpleNode<?> node) {
            return ImmutableNodes.leafNode(node.getNodeType(), node.getValue());
        }

    }

    private static final class LeafListEntryNormalization extends SimpleTypeNormalization<NodeWithValue> {

        public LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null),potential);
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

    private static abstract class CompositeNodeNormalizationOperation<T extends PathArgument> extends
    DataNormalizationOperation<T> {

        protected CompositeNodeNormalizationOperation(final T identifier, final DataSchemaNode schema) {
            super(identifier,schema);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public final NormalizedNode<?, ?> normalize(final Node<?> legacyData) {
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
            return builder.build();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode);

    }

    private static abstract class DataContainerNormalizationOperation<T extends PathArgument> extends
    CompositeNodeNormalizationOperation<T> {

        private final DataNodeContainer schema;
        private final Map<QName, DataNormalizationOperation<?>> byQName;
        private final Map<PathArgument, DataNormalizationOperation<?>> byArg;

        protected DataContainerNormalizationOperation(final T identifier, final DataNodeContainer schema, final DataSchemaNode node) {
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

        private DataNormalizationOperation<?> fromLocalSchema(final PathArgument child) throws DataNormalizationException {
            if (child instanceof AugmentationIdentifier) {
                return fromSchemaAndQNameChecked(schema, ((AugmentationIdentifier) child).getPossibleChildNames()
                        .iterator().next());
            }
            return fromSchemaAndQNameChecked(schema, child.getNodeType());
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

        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema2, final QName child) throws DataNormalizationException {
            return fromSchemaAndQNameChecked(schema2, child);
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
            super(identifier, schema,schema);
            keyDefinition = schema.getKeyDefinition();
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
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

    private static final class UnkeyedListItemNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected UnkeyedListItemNormalization(final ListSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema,schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
            return Builders.unkeyedListEntryBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.unkeyedListEntryBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }

    }

    private static final class ContainerNormalization extends DataContainerNormalizationOperation<NodeIdentifier> {

        protected ContainerNormalization(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()),schema, schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
            return Builders.containerBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.containerBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }

    }

    private static abstract class MixinNormalizationOp<T extends PathArgument> extends
    CompositeNodeNormalizationOperation<T> {

        protected MixinNormalizationOp(final T identifier, final DataSchemaNode schema) {
            super(identifier,schema);
        }

        @Override
        public final boolean isMixin() {
            return true;
        }

    }


    private static final class OrderedLeafListMixinNormalization extends UnorderedLeafListMixinNormalization {


        public OrderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
            return Builders.orderedLeafSetBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.orderedLeafSetBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class UnorderedLeafListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final DataNormalizationOperation<?> innerOp;

        public UnorderedLeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()),potential);
            innerOp = new LeafListEntryNormalization(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
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

    private static final class AugmentationNormalization extends DataContainerNormalizationOperation<AugmentationIdentifier> {

        public AugmentationNormalization(final AugmentationSchema augmentation, final DataNodeContainer schema) {
            //super();
            super(augmentationIdentifierFrom(augmentation), augmentationProxy(augmentation,schema),null);
        }

        @Override
        public boolean isMixin() {
            return true;
        }



        @Override
        protected DataNormalizationOperation<?> fromLocalSchemaAndQName(final DataNodeContainer schema, final QName child)
                throws DataNormalizationException {
            Optional<DataSchemaNode> potential = findChildSchemaNode(schema, child);
            if (!potential.isPresent()) {
                return null;
            }

            DataSchemaNode result = potential.get();
            // We try to look up if this node was added by augmentation
            if ((schema instanceof DataSchemaNode) && result.isAugmenting()) {
                return fromAugmentation(schema, (AugmentationTarget) schema, result);
            }
            return fromDataSchemaNode(result);
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

    private static class UnorderedMapMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ListItemNormalization innerNode;

        public UnorderedMapMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()),list);
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


    private static class UnkeyedListMixinNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final UnkeyedListItemNormalization innerNode;

        public UnkeyedListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()),list);
            this.innerNode = new UnkeyedListItemNormalization(list);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.unkeyedListBuilder().withNodeIdentifier(getIdentifier());
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

        public OrderedMapMixinNormalization(final ListSchemaNode list) {
            super(list);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final CompositeNode compositeNode) {
            return Builders.orderedMapBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.orderedMapBuilder().withNodeIdentifier(getIdentifier()).build();
        }

    }

    private static class ChoiceNodeNormalization extends MixinNormalizationOp<NodeIdentifier> {

        private final ImmutableMap<QName, DataNormalizationOperation<?>> byQName;
        private final ImmutableMap<PathArgument, DataNormalizationOperation<?>> byArg;

        protected ChoiceNodeNormalization(final ChoiceSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()),schema);
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
        protected NormalizedNodeContainerBuilder<?, ?, ?, ?> createBuilder(final CompositeNode compositeNode) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    private static class AnyXmlNormalization extends DataNormalizationOperation<NodeIdentifier> {

        protected AnyXmlNormalization( final AnyXmlSchemaNode schema) {
            super( new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        public DataNormalizationOperation<?> getChild( final PathArgument child ) throws DataNormalizationException {
            return null;
        }

        @Override
        public DataNormalizationOperation<?> getChild( final QName child ) throws DataNormalizationException {
            return null;
        }

        @Override
        public NormalizedNode<?, ?> normalize( final Node<?> legacyData ) {
            NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> builder =
                    Builders.anyXmlBuilder().withNodeIdentifier(
                            new NodeIdentifier( legacyData.getNodeType() ) );
            // Will be removed
//            builder.withValue(legacyData);
            return builder.build();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public NormalizedNode<?, ?> createDefault( final PathArgument currentArg ) {
            return null;
        }
    }

    private static final Optional<DataSchemaNode> findChildSchemaNode(final DataNodeContainer parent,final QName child) {
        DataSchemaNode potential = parent.getDataChildByName(child);
        if (potential == null) {
            Iterable<ChoiceSchemaNode> choices = FluentIterable.from(parent.getChildNodes()).filter(ChoiceSchemaNode.class);
            potential = findChoice(choices, child);
        }
        return Optional.fromNullable(potential);
    }

    private static DataNormalizationOperation<?> fromSchemaAndQNameChecked(final DataNodeContainer schema,
            final QName child) throws DataNormalizationException {

        Optional<DataSchemaNode> potential = findChildSchemaNode(schema, child);
        if (!potential.isPresent()) {
            throw new DataNormalizationException(String.format("Supplied QName %s is not valid according to schema %s, potential children nodes: %s", child, schema,schema.getChildNodes()));
        }

        DataSchemaNode result = potential.get();
        // We try to look up if this node was added by augmentation
        if ((schema instanceof DataSchemaNode) && result.isAugmenting()) {
            return fromAugmentation(schema, (AugmentationTarget) schema, result);
        }
        return fromDataSchemaNode(result);
    }

    private static ChoiceSchemaNode findChoice(final Iterable<ChoiceSchemaNode> choices, final QName child) {
        ChoiceSchemaNode foundChoice = null;
        choiceLoop: for (ChoiceSchemaNode choice : choices) {
            for (ChoiceCaseNode caze : choice.getCases()) {
                if (findChildSchemaNode(caze, child).isPresent()) {
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
        return new AugmentationIdentifier(potentialChildren.build());
    }

    private static DataNodeContainer augmentationProxy(final AugmentationSchema augmentation, final DataNodeContainer schema) {
        Set<DataSchemaNode> children = new HashSet<>();
        for (DataSchemaNode augNode : augmentation.getChildNodes()) {
            children.add(schema.getDataChildByName(augNode.getQName()));
        }
        return new DataSchemaContainerProxy(children);
    }

    /**
     * Returns a DataNormalizationOperation for provided child node
     *
     * If supplied child is added by Augmentation this operation returns
     * a DataNormalizationOperation for augmentation,
     * otherwise returns a DataNormalizationOperation for child as
     * call for {@link #fromDataSchemaNode(DataSchemaNode)}.
     *
     *
     * @param parent
     * @param parentAug
     * @param child
     * @return
     */
    private static DataNormalizationOperation<?> fromAugmentation(final DataNodeContainer parent,
            final AugmentationTarget parentAug, final DataSchemaNode child) {
        AugmentationSchema augmentation = null;
        for (AugmentationSchema aug : parentAug.getAvailableAugmentations()) {
            DataSchemaNode potential = aug.getDataChildByName(child.getQName());
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
        } else if (potential instanceof AnyXmlSchemaNode) {
            return new AnyXmlNormalization( (AnyXmlSchemaNode) potential);
        }
        return null;
    }

    private static DataNormalizationOperation<?> fromListSchemaNode(final ListSchemaNode potential) {
        List<QName> keyDefinition = potential.getKeyDefinition();
        if(keyDefinition == null || keyDefinition.isEmpty()) {
            return new UnkeyedListMixinNormalization(potential);
        }
        if(potential.isUserOrdered()) {
            return new OrderedMapMixinNormalization(potential);
        }
        return new UnorderedMapMixinNormalization(potential);
    }

    private static DataNormalizationOperation<?> fromLeafListSchemaNode(final LeafListSchemaNode potential) {
        if(potential.isUserOrdered()) {
            return new OrderedLeafListMixinNormalization(potential);
        }
        return new UnorderedLeafListMixinNormalization(potential);
    }


    public static DataNormalizationOperation<?> from(final SchemaContext ctx) {
        return new ContainerNormalization(ctx);
    }

    public abstract NormalizedNode<?, ?> createDefault(PathArgument currentArg);
}
