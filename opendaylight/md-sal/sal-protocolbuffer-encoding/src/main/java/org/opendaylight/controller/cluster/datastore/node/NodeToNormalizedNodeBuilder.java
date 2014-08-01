/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * NormalizedNodeBuilder is a builder that walks through a tree like structure and constructs a
 * NormalizedNode from it.
 * <p/>
 * A large part of this code has been copied over from a similar class in sal-common-impl which was
 * originally supposed to convert a CompositeNode to NormalizedNode
 *
 * @param <T>
 */
public abstract class NodeToNormalizedNodeBuilder<T extends PathArgument>
    implements Identifiable<T> {

    private final T identifier;

    protected static final Logger logger = LoggerFactory
        .getLogger(NodeToNormalizedNodeBuilder.class);

    @Override
    public T getIdentifier() {
        return identifier;
    }

    ;

    protected NodeToNormalizedNodeBuilder(final T identifier) {
        super();
        this.identifier = identifier;

    }

    /**
     * @return Should return true if the node that this operation corresponds to is a mixin
     */
    public boolean isMixin() {
        return false;
    }


    /**
     * @return Should return true if the node that this operation corresponds to has a 'key'
     * associated with it. This is typically true for a list-item or leaf-list entry in yang
     */
    public boolean isKeyedEntry() {
        return false;
    }

    protected Set<QName> getQNameIdentifiers() {
        return Collections.singleton(identifier.getNodeType());
    }

    public abstract NodeToNormalizedNodeBuilder<?> getChild(
        final PathArgument child);

    public abstract NodeToNormalizedNodeBuilder<?> getChild(QName child);

    public abstract NormalizedNode<?, ?> normalize(QName nodeType, Node node);



    private static abstract class SimpleTypeNormalization<T extends PathArgument>
        extends NodeToNormalizedNodeBuilder<T> {

        protected SimpleTypeNormalization(final T identifier) {
            super(identifier);
        }

        @Override
        public NormalizedNode<?, ?> normalize(final QName nodeType,
            final Node node) {
            checkArgument(node != null);
            return normalizeImpl(nodeType, node);
        }

        protected abstract NormalizedNode<?, ?> normalizeImpl(QName nodeType,
            Node node);

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            return null;
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            return null;
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            // TODO Auto-generated method stub
            return null;
        }

    }


    private static final class LeafNormalization extends
        SimpleTypeNormalization<NodeIdentifier> {

        private final LeafSchemaNode schema;

        protected LeafNormalization(final LeafSchemaNode schema, final NodeIdentifier identifier) {
            super(identifier);
            this.schema = schema;
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final QName nodeType,
            final Node node) {
            Object value = NodeValueCodec.toTypeSafeValue(this.schema, this.schema.getType(), node);
            return ImmutableNodes.leafNode(nodeType, value);

        }

    }


    private static final class LeafListEntryNormalization extends
        SimpleTypeNormalization<NodeWithValue> {

        private final LeafListSchemaNode schema;

        public LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null));
            this.schema = potential;
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final QName nodeType,
            final Node node) {
            final Object data = node.getValue();
            if (data == null) {
                Preconditions.checkArgument(false,
                    "No data available in leaf list entry for " + nodeType);
            }

            Object value = NodeValueCodec.toTypeSafeValue(this.schema, this.schema.getType(), node);

            NodeWithValue nodeId = new NodeWithValue(nodeType, value);
            return Builders.leafSetEntryBuilder().withNodeIdentifier(nodeId)
                .withValue(value).build();
        }


        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }


    private static abstract class NodeToNormalizationNodeOperation<T extends PathArgument>
        extends NodeToNormalizedNodeBuilder<T> {

        protected NodeToNormalizationNodeOperation(final T identifier) {
            super(identifier);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public final NormalizedNodeContainer<?, ?, ?> normalize(
            final QName nodeType, final Node node) {
            checkArgument(node != null);

            if (!node.getType().equals(AugmentationNode.class.getSimpleName())
                && !node.getType().equals(ContainerNode.class.getSimpleName())
                && !node.getType().equals(MapNode.class.getSimpleName())) {
                checkArgument(nodeType != null);
            }

            NormalizedNodeContainerBuilder builder = createBuilder(node);

            Set<NodeToNormalizedNodeBuilder<?>> usedMixins = new HashSet<>();

            logNode(node);

            if (node.getChildCount() == 0 && (
                node.getType().equals(LeafSetEntryNode.class.getSimpleName())
                    || node.getType().equals(LeafNode.class.getSimpleName()))) {
                PathArgument childPathArgument =
                    NodeIdentifierFactory.getArgument(node.getPath());

                final NormalizedNode child;
                if (childPathArgument instanceof NodeWithValue) {
                    final NodeWithValue nodeWithValue =
                        new NodeWithValue(childPathArgument.getNodeType(),
                            node.getValue());
                    child =
                        Builders.leafSetEntryBuilder()
                            .withNodeIdentifier(nodeWithValue)
                            .withValue(node.getValue()).build();
                } else {
                    child =
                        ImmutableNodes.leafNode(childPathArgument.getNodeType(),
                            node.getValue());
                }
                builder.addChild(child);
            }

            final List<Node> children = node.getChildList();
            for (Node nodeChild : children) {

                PathArgument childPathArgument =
                    NodeIdentifierFactory.getArgument(nodeChild.getPath());

                QName childNodeType = null;
                NodeToNormalizedNodeBuilder childOp = null;

                if (childPathArgument instanceof AugmentationIdentifier) {
                    childOp = getChild(childPathArgument);
                    checkArgument(childOp instanceof AugmentationNormalization, childPathArgument);
                } else {
                    childNodeType = childPathArgument.getNodeType();
                    childOp = getChild(childNodeType);
                }
                // We skip unknown nodes if this node is mixin since
                // it's nodes and parent nodes are interleaved
                if (childOp == null && isMixin()) {
                    continue;
                } else if (childOp == null) {
                    logger.error(
                        "childOp is null and this operation is not a mixin : this = {}",
                        this.toString());
                }

                checkArgument(childOp != null,
                    "Node %s is not allowed inside %s",
                    childNodeType, getIdentifier());

                if (childOp.isMixin()) {
                    if (usedMixins.contains(childOp)) {
                        // We already run / processed that mixin, so to avoid
                        // duplicate we are
                        // skipping next nodes.
                        continue;
                    }
                    // builder.addChild(childOp.normalize(nodeType, treeCacheNode));
                    final NormalizedNode childNode =
                        childOp.normalize(childNodeType, nodeChild);
                    if (childNode != null)
                        builder.addChild(childNode);
                    usedMixins.add(childOp);
                } else {
                    final NormalizedNode childNode =
                        childOp.normalize(childNodeType, nodeChild);
                    if (childNode != null)
                        builder.addChild(childNode);
                }
            }


            try {
                return (NormalizedNodeContainer<?, ?, ?>) builder.build();
            } catch (Exception e) {
                return null;
            }

        }

        private void logNode(Node node) {
            //let us find out the type of the node
            logger.debug("We got a {} , with identifier {} with {} children",
                node.getType(), node.getPath(),
                node.getChildList());
        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(
            final Node node);

    }


    private static abstract class DataContainerNormalizationOperation<T extends PathArgument>
        extends NodeToNormalizationNodeOperation<T> {

        private final DataNodeContainer schema;
        private final Map<QName, NodeToNormalizedNodeBuilder<?>> byQName;
        private final Map<PathArgument, NodeToNormalizedNodeBuilder<?>> byArg;

        protected DataContainerNormalizationOperation(final T identifier,
            final DataNodeContainer schema) {
            super(identifier);
            this.schema = schema;
            this.byArg = new ConcurrentHashMap<>();
            this.byQName = new ConcurrentHashMap<>();
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            NodeToNormalizedNodeBuilder<?> potential = byArg.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromSchema(schema, child);
            return register(potential);
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            if (child == null) {
                return null;
            }

            NodeToNormalizedNodeBuilder<?> potential = byQName.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromSchemaAndPathArgument(schema, child);
            return register(potential);
        }

        private NodeToNormalizedNodeBuilder<?> register(
            final NodeToNormalizedNodeBuilder<?> potential) {
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
        private final ListSchemaNode schemaNode;

        protected ListItemNormalization(
            final NodeIdentifierWithPredicates identifier,
            final ListSchemaNode schema) {
            super(identifier, schema);
            this.schemaNode = schema;
            keyDefinition = schema.getKeyDefinition();
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            NodeIdentifierWithPredicates nodeIdentifierWithPredicates =
                (NodeIdentifierWithPredicates) NodeIdentifierFactory
                    .createPathArgument(node
                        .getPath(), schemaNode);
            return Builders.mapEntryBuilder()
                .withNodeIdentifier(
                    nodeIdentifierWithPredicates
            );
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode>
                builder =
                Builders.mapEntryBuilder().withNodeIdentifier(
                    (NodeIdentifierWithPredicates) currentArg);
            for (Entry<QName, Object> keyValue : ((NodeIdentifierWithPredicates) currentArg)
                .getKeyValues().entrySet()) {
                if (keyValue.getValue() == null) {
                    throw new NullPointerException(
                        "Null value found for path : "
                            + currentArg);
                }
                builder.addChild(Builders.leafBuilder()
                    //
                    .withNodeIdentifier(new NodeIdentifier(keyValue.getKey()))
                    .withValue(keyValue.getValue()).build());
            }
            return builder.build();
        }


        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }


    private static final class ContainerNormalization extends
        DataContainerNormalizationOperation<NodeIdentifier> {

        protected ContainerNormalization(final ContainerSchemaNode schema) {
            super(new NodeIdentifier(schema.getQName()), schema);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            return Builders.containerBuilder()
                .withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            return Builders.containerBuilder()
                .withNodeIdentifier((NodeIdentifier) currentArg).build();
        }

    }


    private static abstract class MixinNormalizationOp<T extends PathArgument>
        extends NodeToNormalizationNodeOperation<T> {

        protected MixinNormalizationOp(final T identifier) {
            super(identifier);
        }

        @Override
        public final boolean isMixin() {
            return true;
        }

    }


    private static final class LeafListMixinNormalization extends
        MixinNormalizationOp<NodeIdentifier> {

        private final NodeToNormalizedNodeBuilder<?> innerOp;

        public LeafListMixinNormalization(final LeafListSchemaNode potential) {
            super(new NodeIdentifier(potential.getQName()));
            innerOp = new LeafListEntryNormalization(potential);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            return Builders.leafSetBuilder()
                .withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            return Builders.leafSetBuilder().withNodeIdentifier(getIdentifier())
                .build();
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            if (child instanceof NodeWithValue) {
                return innerOp;
            }
            return null;
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerOp;
            }
            return null;
        }

    }


    private static final class AugmentationNormalization extends
        MixinNormalizationOp<AugmentationIdentifier> {

        private final Map<QName, NodeToNormalizedNodeBuilder<?>> byQName;
        private final Map<PathArgument, NodeToNormalizedNodeBuilder<?>> byArg;

        public AugmentationNormalization(final AugmentationSchema augmentation,
            final DataNodeContainer schema) {
            super(augmentationIdentifierFrom(augmentation));

            ImmutableMap.Builder<QName, NodeToNormalizedNodeBuilder<?>>
                byQNameBuilder =
                ImmutableMap.builder();
            ImmutableMap.Builder<PathArgument, NodeToNormalizedNodeBuilder<?>>
                byArgBuilder =
                ImmutableMap.builder();

            for (DataSchemaNode augNode : augmentation.getChildNodes()) {
                DataSchemaNode resolvedNode =
                    schema.getDataChildByName(augNode.getQName());
                NodeToNormalizedNodeBuilder<?> resolvedOp =
                    fromDataSchemaNode(resolvedNode);
                byArgBuilder.put(resolvedOp.getIdentifier(), resolvedOp);
                for (QName resQName : resolvedOp.getQNameIdentifiers()) {
                    byQNameBuilder.put(resQName, resolvedOp);
                }
            }
            byQName = byQNameBuilder.build();
            byArg = byArgBuilder.build();

        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            return byArg.get(child);
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            return byQName.get(child);
        }

        @Override
        protected Set<QName> getQNameIdentifiers() {
            return getIdentifier().getPossibleChildNames();
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            return Builders.augmentationBuilder()
                .withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            return Builders.augmentationBuilder()
                .withNodeIdentifier(getIdentifier())
                .build();
        }

    }


    private static final class ListMixinNormalization extends
        MixinNormalizationOp<NodeIdentifier> {

        private final ListItemNormalization innerNode;

        public ListMixinNormalization(final ListSchemaNode list) {
            super(new NodeIdentifier(list.getQName()));
            this.innerNode =
                new ListItemNormalization(new NodeIdentifierWithPredicates(
                    list.getQName(), Collections.<QName, Object>emptyMap()),
                    list);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            return Builders.mapBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            return Builders.mapBuilder().withNodeIdentifier(getIdentifier())
                .build();
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            if (child.getNodeType().equals(getIdentifier().getNodeType())) {
                return innerNode;
            }
            return null;
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            if (getIdentifier().getNodeType().equals(child)) {
                return innerNode;
            }
            return null;
        }

    }


    private static class ChoiceNodeNormalization extends
        MixinNormalizationOp<NodeIdentifier> {

        private final ImmutableMap<QName, NodeToNormalizedNodeBuilder<?>>
            byQName;
        private final ImmutableMap<PathArgument, NodeToNormalizedNodeBuilder<?>>
            byArg;

        protected ChoiceNodeNormalization(
            final org.opendaylight.yangtools.yang.model.api.ChoiceNode schema) {
            super(new NodeIdentifier(schema.getQName()));
            ImmutableMap.Builder<QName, NodeToNormalizedNodeBuilder<?>>
                byQNameBuilder =
                ImmutableMap.builder();
            ImmutableMap.Builder<PathArgument, NodeToNormalizedNodeBuilder<?>>
                byArgBuilder =
                ImmutableMap.builder();

            for (ChoiceCaseNode caze : schema.getCases()) {
                for (DataSchemaNode cazeChild : caze.getChildNodes()) {
                    NodeToNormalizedNodeBuilder<?> childOp =
                        fromDataSchemaNode(cazeChild);
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
        public NodeToNormalizedNodeBuilder<?> getChild(
            final PathArgument child) {
            return byArg.get(child);
        }

        @Override
        public NodeToNormalizedNodeBuilder<?> getChild(final QName child) {
            return byQName.get(child);
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(
            final Node node) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(
            final PathArgument currentArg) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier())
                .build();
        }
    }

    /**
     * Find an appropriate NormalizedNodeBuilder using both the schema and the
     * Path Argument
     *
     * @param schema
     * @param child
     * @return
     */
    public static NodeToNormalizedNodeBuilder<?> fromSchemaAndPathArgument(
        final DataNodeContainer schema, final QName child) {
        DataSchemaNode potential = schema.getDataChildByName(child);
        if (potential == null) {
            Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode>
                choices =
                FluentIterable.from(schema.getChildNodes()).filter(
                    org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }
        if (potential == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("BAD CHILD = {}", child.toString());
            }
        }

        checkArgument(potential != null,
            "Supplied QName %s is not valid according to schema %s", child,
            schema);

        // If the schema in an instance of DataSchemaNode and the potential
        // is augmenting something then there is a chance that this may be
        // and augmentation node
        if ((schema instanceof DataSchemaNode)
            && potential.isAugmenting()) {

            AugmentationNormalization augmentation =
                fromAugmentation(schema, (AugmentationTarget) schema,
                    potential);

            // If an augmentation normalization (builder) is not found then
            // we fall through to the regular processing
            if(augmentation != null){
                return augmentation;
            }
        }
        return fromDataSchemaNode(potential);
    }

    /**
     * Given a bunch of choice nodes and a the name of child find a choice node for that child which
     * has a non-null value
     *
     * @param choices
     * @param child
     * @return
     */
    private static org.opendaylight.yangtools.yang.model.api.ChoiceNode findChoice(
        final Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices,
        final QName child) {
        org.opendaylight.yangtools.yang.model.api.ChoiceNode foundChoice = null;
        choiceLoop:
        for (org.opendaylight.yangtools.yang.model.api.ChoiceNode choice : choices) {
            for (ChoiceCaseNode caze : choice.getCases()) {
                if (caze.getDataChildByName(child) != null) {
                    foundChoice = choice;
                    break choiceLoop;
                }
            }
        }
        return foundChoice;
    }


    /**
     * Create an AugmentationIdentifier based on the AugmentationSchema
     *
     * @param augmentation
     * @return
     */
    public static AugmentationIdentifier augmentationIdentifierFrom(
        final AugmentationSchema augmentation) {
        ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(potentialChildren.build());
    }

    /**
     * Create an AugmentationNormalization based on the schema of the DataContainer, the
     * AugmentationTarget and the potential schema node
     *
     * @param schema
     * @param augments
     * @param potential
     * @return
     */
    private static AugmentationNormalization fromAugmentation(
        final DataNodeContainer schema, final AugmentationTarget augments,
        final DataSchemaNode potential) {
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

    /**
     * @param schema
     * @param child
     * @return
     */
    private static NodeToNormalizedNodeBuilder<?> fromSchema(
        final DataNodeContainer schema, final PathArgument child) {
        if (child instanceof AugmentationIdentifier) {
            QName childQName = ((AugmentationIdentifier) child)
                .getPossibleChildNames().iterator().next();

            return fromSchemaAndPathArgument(schema, childQName);
        }
        return fromSchemaAndPathArgument(schema, child.getNodeType());
    }

    public static NodeToNormalizedNodeBuilder<?> fromDataSchemaNode(
        final DataSchemaNode potential) {
        if (potential instanceof ContainerSchemaNode) {
            return new ContainerNormalization((ContainerSchemaNode) potential);
        } else if (potential instanceof ListSchemaNode) {
            return new ListMixinNormalization((ListSchemaNode) potential);
        } else if (potential instanceof LeafSchemaNode) {
            return new LeafNormalization((LeafSchemaNode) potential,
                new NodeIdentifier(potential.getQName()));
        } else if (potential instanceof org.opendaylight.yangtools.yang.model.api.ChoiceNode) {
            return new ChoiceNodeNormalization(
                (org.opendaylight.yangtools.yang.model.api.ChoiceNode) potential);
        } else if (potential instanceof LeafListSchemaNode) {
            return new LeafListMixinNormalization(
                (LeafListSchemaNode) potential);
        }
        return null;
    }

    public static NodeToNormalizedNodeBuilder<?> from(final SchemaContext ctx) {
        return new ContainerNormalization(ctx);
    }

    public abstract NormalizedNode<?, ?> createDefault(PathArgument currentArg);

}
