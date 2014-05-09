package org.opendaylight.controller.datastore.infinispan;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.opendaylight.controller.datastore.infinispan.utils.InfinispanTreeWrapper;
import org.opendaylight.controller.datastore.infinispan.utils.NamespacePrefixMapper;
import org.opendaylight.controller.datastore.infinispan.utils.NodeIdentifierFactory;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
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
 * DataNormalizationOperation is a builder that walks through a tree like structure based on Infinispan TreeCache
 * and constructs a NormalizedNode from it.
 *
 * A large part of this code has been copied over from a similar class in sal-common-impl which was originally
 * supposed to convert a CompositeNode to NormalizedNode
 * @param <T>
 */
public abstract class  DataNormalizationOperation<T extends PathArgument> implements Identifiable<T> {

    private final T identifier;
    protected final InfinispanTreeWrapper infinispanTreeWrapper;
    private static final Logger logger = LoggerFactory.getLogger(DataNormalizationOperation.class);

    @Override
    public T getIdentifier() {
        return identifier;
    };

    protected DataNormalizationOperation(final T identifier) {
        super();
        this.identifier = identifier;
        this.infinispanTreeWrapper = new InfinispanTreeWrapper();
    }

    /**
     *
     * @return Should return true if the node that this operation corresponds to is a mixin
     */
    public boolean isMixin() {
        return false;
    }


    /**
     *
     * @return Should return true if the node that this operation corresponds to has a 'key' associated with it. This is
     * typically true for a list-item or leaf-list entry in yang
     */
    public boolean isKeyedEntry() {
        return false;
    }

    protected Set<QName> getQNameIdentifiers() {
        return Collections.singleton(identifier.getNodeType());
    }

    public abstract DataNormalizationOperation<?> getChild(final PathArgument child);

    public abstract DataNormalizationOperation<?> getChild(QName child);

    public abstract NormalizedNode<?, ?> normalize(QName nodeType, org.infinispan.tree.Node legacyData);

    protected PathArgument getNodePathArgument(Fqn fqn){
        String lastPath = fqn.getLastElement().toString();
        String id = NamespacePrefixMapper.get().fromFqn(lastPath);
        return NodeIdentifierFactory.getArgument(id);
    }

    protected PathArgument getNodePathArgument(String nodeIdentifier){
        String id = NamespacePrefixMapper.get().fromFqn(nodeIdentifier);
        return NodeIdentifierFactory.getArgument(id);
    }


    private static abstract class SimpleTypeNormalization<T extends PathArgument> extends DataNormalizationOperation<T> {

        protected SimpleTypeNormalization(final T identifier) {
            super(identifier);
        }

        @Override
        public NormalizedNode<?, ?> normalize(final QName nodeType, final org.infinispan.tree.Node legacyData) {
            checkArgument(legacyData != null);
            return normalizeImpl(nodeType, legacyData);
        }

        protected abstract NormalizedNode<?, ?> normalizeImpl(QName nodeType, Node node);

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
        protected NormalizedNode<?, ?> normalizeImpl(final QName nodeType, final Node node) {
            return ImmutableNodes.leafNode(nodeType, infinispanTreeWrapper.readValue(node));
        }

    }

    private static final class LeafListEntryNormalization extends SimpleTypeNormalization<NodeWithValue> {

        public LeafListEntryNormalization(final LeafListSchemaNode potential) {
            super(new NodeWithValue(potential.getQName(), null));
        }

        @Override
        protected NormalizedNode<?, ?> normalizeImpl(final QName nodeType, final Node node) {
            final Object data = infinispanTreeWrapper.readValue(node);
            if(data == null){
                Preconditions.checkArgument(false, "No data available in leaf list entry for " + nodeType);
            }
            NodeWithValue nodeId = new NodeWithValue(nodeType, data);
            return Builders.leafSetEntryBuilder().withNodeIdentifier(nodeId).withValue(data).build();
        }


        @Override
        public boolean isKeyedEntry() {
            return true;
        }
    }

    private static abstract class TreeCacheNodeNormalizationOperation<T extends PathArgument> extends
            DataNormalizationOperation<T> {

        protected TreeCacheNodeNormalizationOperation(final T identifier) {
            super(identifier);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public final NormalizedNodeContainer<?, ?, ?> normalize(final QName nodeType, final org.infinispan.tree.Node legacyData) {
            checkArgument(legacyData != null);
            checkArgument(legacyData instanceof org.infinispan.tree.Node, "Node %s should be composite", legacyData);
            org.infinispan.tree.Node treeCacheNode = legacyData;
            NormalizedNodeContainerBuilder builder = createBuilder(treeCacheNode);

            Set<DataNormalizationOperation<?>> usedMixins = new HashSet<>();

            final Map<String, Object> data = treeCacheNode.getData();
            for(String key : data.keySet()){
                PathArgument childPathArgument = getNodePathArgument(key);
                NormalizedNode child = null;
                if(childPathArgument instanceof NodeWithValue){
                    final NodeWithValue nodeWithValue = new NodeWithValue(childPathArgument.getNodeType(), data.get(key));
                    child = Builders.leafSetEntryBuilder().withNodeIdentifier(nodeWithValue).withValue(data.get(key)).build();
                } else {
                    child = ImmutableNodes.leafNode(childPathArgument.getNodeType(), data.get(key));
                }
                builder.addChild(child);
            }

            final Set<Node> children = infinispanTreeWrapper.getChildren(treeCacheNode);
            for (org.infinispan.tree.Node childLegacy : children) {
                Fqn childFqn = childLegacy.getFqn();
                PathArgument childPathArgument = getNodePathArgument(childFqn);

                QName childNodeType = null;
                DataNormalizationOperation childOp = null;


                if(childPathArgument instanceof AugmentationIdentifier){
                    childOp = getChild(childPathArgument);
                } else {
                    childNodeType = childPathArgument.getNodeType();
                    childOp = getChild(childNodeType);
                }
                // We skip unknown nodes if this node is mixin since
                // it's nodes and parent nodes are interleaved
                if (childOp == null && isMixin()) {
                    continue;
                } else if(childOp == null){
                    logger.error("childOp is null and this operation is not a mixin : this = {}", this.toString());
                }


                checkArgument(childOp != null, "Node %s is not allowed inside %s", childNodeType,
                        getIdentifier());
                if (childOp.isMixin()) {
                    if (usedMixins.contains(childOp)) {
                        // We already run / processed that mixin, so to avoid
                        // dupliciry we are
                        // skiping next nodes.
                        continue;
                    }
                    //builder.addChild(childOp.normalize(nodeType, treeCacheNode));
                    final NormalizedNode childNode = childOp.normalize(childNodeType, childLegacy);
                    if(childNode != null)
                        builder.addChild(childNode);
                    usedMixins.add(childOp);
                } else {
                    final NormalizedNode childNode = childOp.normalize(childNodeType, childLegacy);
                    if(childNode != null)
                        builder.addChild(childNode);
                }
            }

            try {
                return (NormalizedNodeContainer<?, ?, ?>) builder.build();
            } catch (Exception e){
                //throw new RuntimeException(e);
                //logger.error("Error : {}", e.getMessage());
                return null;
            }

        }

        @SuppressWarnings("rawtypes")
        protected abstract NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode);

    }

    private static abstract class DataContainerNormalizationOperation<T extends PathArgument> extends
            TreeCacheNodeNormalizationOperation<T> {

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
        public DataNormalizationOperation<?> getChild(final PathArgument child) {
            DataNormalizationOperation<?> potential = byArg.get(child);
            if (potential != null) {
                return potential;
            }
            potential = fromSchema(schema, child);
            return register(potential);
        }

        @Override
        public DataNormalizationOperation<?> getChild(final QName child) {
            if(child == null){
                return null;
            }

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
        private final ListSchemaNode schemaNode;

        protected ListItemNormalization(final NodeIdentifierWithPredicates identifier, final ListSchemaNode schema) {
            super(identifier, schema);
            this.schemaNode = schema;
            keyDefinition = schema.getKeyDefinition();
        }

        @Override
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
            return Builders.mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates) getNodePathArgument(treeCacheNode.getFqn()));
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder = Builders
                    .mapEntryBuilder().withNodeIdentifier((NodeIdentifierWithPredicates) currentArg);
            for (Entry<QName, Object> keyValue : ((NodeIdentifierWithPredicates) currentArg).getKeyValues().entrySet()) {
                if(keyValue.getValue() == null){
                    throw new NullPointerException("Null value found for path : " + currentArg);
                }
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
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
            return Builders.containerBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.containerBuilder().withNodeIdentifier((NodeIdentifier) currentArg).build();
        }

    }

    private static abstract class MixinNormalizationOp<T extends PathArgument> extends
            TreeCacheNodeNormalizationOperation<T> {

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
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
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
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
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
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
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
        protected NormalizedNodeContainerBuilder createBuilder(final org.infinispan.tree.Node treeCacheNode) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier());
        }

        @Override
        public NormalizedNode<?, ?> createDefault(final PathArgument currentArg) {
            return Builders.choiceBuilder().withNodeIdentifier(getIdentifier()).build();
        }
    }

    public static DataNormalizationOperation<?> fromSchemaAndPathArgument(final DataNodeContainer schema,
            final QName child) {
        DataSchemaNode potential = schema.getDataChildByName(child);
        if (potential == null) {
            Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices = FluentIterable.from(
                    schema.getChildNodes()).filter(org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }
        if(potential == null){
            if(logger.isTraceEnabled()){
                logger.trace("BAD CHILD = {}", child.toString());
            }
        }

        checkArgument(potential != null, "Supplied QName %s is not valid according to schema %s", child , schema);
        if ((schema instanceof DataSchemaNode) && !((DataSchemaNode) schema).isAugmenting() && potential.isAugmenting()) {
            return fromAugmentation(schema, (AugmentationTarget) schema, potential);
        }
        return fromDataSchemaNode(potential);
    }

    /**
     * Given a bunch of choice nodes and a the name of child find a choice node for that child which has a non-null value
     *
     * @param choices
     * @param child
     * @return
     */
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


    /**
     * Create an AugmentationIdentifier based on the AugmentationSchema
     *
     * @param augmentation
     * @return
     */
    public static AugmentationIdentifier augmentationIdentifierFrom(final AugmentationSchema augmentation) {
        ImmutableSet.Builder<QName> potentialChildren = ImmutableSet.builder();
        for (DataSchemaNode child : augmentation.getChildNodes()) {
            potentialChildren.add(child.getQName());
        }
        return new AugmentationIdentifier(null, potentialChildren.build());
    }

    /**
     * Create an AugmentationNormalization based on the schema of the DataContainer, the AugmentationTarget and the
     * potential schema node
     *
     * @param schema
     * @param augments
     * @param potential
     * @return
     */
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

    /**
     *
     * @param schema
     * @param child
     * @return
     */
    private static DataNormalizationOperation<?> fromSchema(final DataNodeContainer schema, final PathArgument child) {
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
