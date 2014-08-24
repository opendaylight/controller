/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CompositeNodeCompatibility provides a way to create a protocol buffer node
 * that is compatible with CompositeNode. The idea here is that if we had a
 * serialized object which adhered to the CompositeNode interface we could
 * leverage the {@link org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer }
 * DataNormalizer from sal-common-impl to create a NormalizedNode from that
 * serialized object.
 */
public class CompositeNodeCompatibility {

    /**
     * Convert NormalizedNode into a protocol buffer node which is very similar
     * in structure to a CompositeNode
     *
     * @param node NormalizedNode
     * @return
     */
    public static NormalizedNodeMessages.Node toNode(final NormalizedNode<?, ?> node){
        return new NormalizedNodeToNodeConverter(node).toNode();
    }

    /**
     * Given a TopLevelNode get the string representation of a QName used in the
     * context of that TopLevelNode
     *
     * @param topLevelNode
     * @param nodeType
     * @return
     */
    public static String toString(TopLevelNode topLevelNode, NormalizedNodeMessages.QName nodeType){
        Preconditions.checkArgument(!"".equals(nodeType.getLocalName()), "nodeType.localName cannot be empty");
        Preconditions.checkArgument(nodeType.getNamespace() != -1, "nodeType.namespace should be valid");

        return topLevelNode.toString(nodeType);
    }



    /**
     * Create a CompositeNode from a protocol buffer Node
     *
     * @param node Protocol Buffer Node
     * @param schema The schema to be used. This should match the top level node.
     * @return
     */
    public static CompositeNode toComposite(NormalizedNodeMessages.Node node, DataSchemaNode schema){
        Preconditions.checkNotNull(node, "node should not be null");

        return new CompositeNodeWrapper(new TopLevelNode(node), node, null, schema);
    }

    public static NormalizedNode toSimpleNormalizedNode(NormalizedNodeMessages.Node node, DataSchemaNode schema){
        Preconditions.checkNotNull(node, "node should not be null");

        SimpleNodeWrapper simple =
            new SimpleNodeWrapper(new TopLevelNode(node), node, null, schema);

        if(node.getType().equals(LeafNode.class.getSimpleName())){
            return ImmutableNodes.leafNode(simple.getNodeType(),
                simple.getValue());
        } else if(node.getType().equals(LeafSetEntryNode.class.getSimpleName())){
            YangInstanceIdentifier.NodeWithValue
                nodeId = new YangInstanceIdentifier.NodeWithValue(simple.getNodeType(), simple.getValue());
            return Builders.leafSetEntryBuilder().withNodeIdentifier(nodeId).withValue(simple.getValue()).build();
        } else if(node.getType().equals(DataContainerNode.class.getSimpleName())){
            return ImmutableNodes.containerNode(getNodeType(new TopLevelNode(node), node));
        }

        return null;
    }


    private static QName getNodeType(TopLevelNode topLevelNode, NormalizedNodeMessages.Node node){
        if("".equals(node.getNodeType().getLocalName())){
            // Returning null instead of throwing an IllegalArgumentException to
            // not break existing code
            return null;
        }
        return QNameFactory.create(toString(topLevelNode, node.getNodeType()));
    }



    /**
     * CompositeNodeWrapper wraps a protocol buffer node and implements the
     * CompositeNode interface.
     *
     * It also tries to use the schema context to get to the type definitions
     * of the values
     */
    private static class CompositeNodeWrapper implements CompositeNode {

        private final NormalizedNodeMessages.Node node;
        private final CompositeNode parent;
        private final List<Node<?>> value = new ArrayList<>();
        private final QName nodeType;

        private final Logger LOG = LoggerFactory.getLogger(CompositeNodeWrapper.class);
        private final TopLevelNode topLevelNode;

        public CompositeNodeWrapper(TopLevelNode topLevelNode, NormalizedNodeMessages.Node node, CompositeNode parent, DataSchemaNode schema){
            this.topLevelNode = Preconditions.checkNotNull(topLevelNode, "topLevelNode should not be null");
            this.node = Preconditions.checkNotNull(node, "node should not be null");
            this.parent = parent;
            this.nodeType = CompositeNodeCompatibility.getNodeType(topLevelNode, node);

            DataNodeContainer containerSchema = null;

            if(schema instanceof DataNodeContainer){
                containerSchema = (DataNodeContainer) schema;
            } else if(schema != null) {
                Preconditions.checkArgument(schema instanceof AnyXmlSchemaNode,
                    String.format("Invalid schema provided schema = %s, node = %s",
                        schema, node.toString()));
            } else {
                LOG.debug("null schema provided for {}", node);
            }

            for(NormalizedNodeMessages.Node child : node.getChildList()){
                DataSchemaNode childSchema = null;


                if(containerSchema != null) {
                    Optional<DataSchemaNode> optional =
                        findChildSchemaNode(containerSchema,
                            CompositeNodeCompatibility.getNodeType(topLevelNode, child));

                    if (optional.isPresent()) {
                        childSchema = optional.get();
                    }
                }

                if(child.getChildCount() == 0){
                    value.add(new SimpleNodeWrapper(this.topLevelNode, child, this, childSchema));
                } else {
                    value.add(new CompositeNodeWrapper(this.topLevelNode, child, this, childSchema));
                }
            }
        }

        @Override public List<Node<?>> getChildren() {
            return getValue();
        }

        @Override public List<CompositeNode> getCompositesByName(QName qName) {
            throw new UnsupportedOperationException("getCompositesByName");
        }

        @Override public List<CompositeNode> getCompositesByName(String s) {
            throw new UnsupportedOperationException("getCompositesByName");
        }

        @Override public List<SimpleNode<?>> getSimpleNodesByName(QName qName) {
            throw new UnsupportedOperationException("getSimpleNodesByName");
        }

        @Override public List<SimpleNode<?>> getSimpleNodesByName(String s) {
            throw new UnsupportedOperationException("getSimpleNodesByName");
        }

        @Override public CompositeNode getFirstCompositeByName(QName qName) {
            throw new UnsupportedOperationException("getFirstCompositeByName");
        }

        @Override public SimpleNode<?> getFirstSimpleByName(QName qName) {
            for(Node child : value){
                if(child instanceof SimpleNode){
                    if(qName.equals(child.getNodeType())){
                        return (SimpleNode) child;
                    }
                }
            }
            return null;
        }

        @Override public MutableCompositeNode asMutable() {
            throw new UnsupportedOperationException("asMutable");
        }

        @Override public QName getKey() {
            return getNodeType();
        }

        @Override public List<Node<?>> setValue(List<Node<?>> value) {
            throw new UnsupportedOperationException("setValue");
        }

        @Override public int size() {
            return node.getChildCount();
        }

        @Override public boolean isEmpty() {
            return size() == 0;
        }

        @Override public boolean containsKey(Object key) {
            throw new UnsupportedOperationException("containsKey");
        }

        @Override public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("containsValue");
        }

        @Override public List<Node<?>> get(Object key) {
            throw new UnsupportedOperationException("get");
        }

        @Override public List<Node<?>> put(QName key, List<Node<?>> value) {
            throw new UnsupportedOperationException("put");
        }

        @Override public List<Node<?>> remove(Object key) {
            throw new UnsupportedOperationException("remove");
        }

        @Override public void putAll(
            Map<? extends QName, ? extends List<Node<?>>> m) {
            throw new UnsupportedOperationException("putAll");
        }

        @Override public void clear() {
            throw new UnsupportedOperationException("clear");
        }

        @Override public Set<QName> keySet() {
            throw new UnsupportedOperationException("keySet");
        }

        @Override public Collection<List<Node<?>>> values() {
            throw new UnsupportedOperationException("values");
        }

        @Override public Set<Entry<QName, List<Node<?>>>> entrySet() {
            throw new UnsupportedOperationException("entrySet");
        }

        @Override public QName getNodeType() {
            return nodeType;
        }

        @Override public CompositeNode getParent() {
            return parent;
        }

        @Override public List<Node<?>> getValue() {
            return value;
        }

        @Override public ModifyAction getModificationAction() {
            throw new UnsupportedOperationException("getModificationAction");
        }


    }


    /**
     * SimpleNodeWrapper wraps a protocol buffer node and implements SimpleNode
     */
    private static class SimpleNodeWrapper implements SimpleNode {

        private final NormalizedNodeMessages.Node node;
        private final CompositeNode parent;
        private final TopLevelNode topLevelNode;
        private DataSchemaNode dataSchemaNode;
        private TypeDefinition typeDefinition = null;
        private final QName nodeType;

        public SimpleNodeWrapper(TopLevelNode topLevelNode, NormalizedNodeMessages.Node node, CompositeNode parent, DataSchemaNode dataSchemaNode){
            this.topLevelNode = Preconditions.checkNotNull(topLevelNode, "topLevelNode should not be null");
            this.node = Preconditions.checkNotNull(node, "node should not be null");
            this.parent = parent;
            this.dataSchemaNode = dataSchemaNode;
            this.nodeType = CompositeNodeCompatibility.getNodeType(topLevelNode, node);

            // If this is a choice node we need to get to the exact data schema
            // for this node by going through all the cases and finding a match
            if(dataSchemaNode instanceof ChoiceNode){
                ChoiceNode choiceNode = (ChoiceNode) dataSchemaNode;
                for (ChoiceCaseNode caze : choiceNode.getCases()) {
                    for (DataSchemaNode cazeChild : caze.getChildNodes()) {
                        if(cazeChild.getQName().equals(nodeType)){
                            this.dataSchemaNode = cazeChild;
                        }
                    }
                }
            }

            if(this.dataSchemaNode instanceof LeafListSchemaNode){
                typeDefinition = ((LeafListSchemaNode) this.dataSchemaNode).getType();
            } else if(this.dataSchemaNode instanceof LeafSchemaNode){
                typeDefinition = ((LeafSchemaNode) this.dataSchemaNode).getType();
            }
        }

        @Override public MutableSimpleNode asMutable() {
            throw new UnsupportedOperationException("asMutable");
        }

        @Override public QName getNodeType() {
            return nodeType;
        }

        @Override public CompositeNode getParent() {
            return parent;
        }

        @Override public QName getKey() {
            return getNodeType();
        }

        @Override public Object getValue() {
            if(typeDefinition != null){
                return  NodeValueCodec.toTypeSafeValue(dataSchemaNode, typeDefinition, node);
            }

            return node.getValue();

        }

        @Override public Object setValue(Object value) {
            throw new UnsupportedOperationException("setValue");
        }

        @Override public boolean equals(Object o) {
            throw new UnsupportedOperationException("equals");
        }

        @Override public int hashCode() {
            throw new UnsupportedOperationException("hashCode");
        }

        @Override public ModifyAction getModificationAction() {
            throw new UnsupportedOperationException("getModificationAction");
        }
    }

    /**
     * find the DataSchemaNode for the given child
     *
     * If the child is not found in the parents child nodes then we go through
     * all children of the parent which are ChoiceNodes to find one which matches
     * the child QName
     *
     * @param parent
     * @param child
     * @return
     */
    private static final Optional<DataSchemaNode> findChildSchemaNode(final DataNodeContainer parent,final QName child) {
        DataSchemaNode potential = parent.getDataChildByName(child);
        if (potential == null) {
            Iterable<org.opendaylight.yangtools.yang.model.api.ChoiceNode> choices = FluentIterable.from(
                parent.getChildNodes()).filter(org.opendaylight.yangtools.yang.model.api.ChoiceNode.class);
            potential = findChoice(choices, child);
        }
        return Optional.fromNullable(potential);
    }

    /**
     * Given a set of ChoiceNodes finds one which contains a ChoiceCaseNode which
     * has a child that matches the 'child' QName
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
                if (findChildSchemaNode(caze, child).isPresent()) {
                    foundChoice = choice;
                    break choiceLoop;
                }
            }
        }
        return foundChoice;
    }

    private static class NormalizedNodeToNodeConverter {
        private final NormalizedNode<?, ?> node;
        private final Map<URI, Integer> namespaces = new HashMap<>();
        private final Map<Date, Integer> revisions = new HashMap<>();
        private final NormalizedNodeMessages.Node.Builder builder;

        NormalizedNodeToNodeConverter(final NormalizedNode<?, ?> node){
            this.node = Preconditions.checkNotNull(node, "node should not be null");
            this.builder = NormalizedNodeMessages.Node.newBuilder();
        }

        public NormalizedNodeMessages.Node toNode(){
            NormalizedNodeMessages.Node out = toNode(builder, node);
            return out;
        }

        private NormalizedNodeMessages.Node toNode(NormalizedNodeMessages.Node.Builder builder, final NormalizedNode<?, ?> node){
            if (node instanceof MixinNode) {
                /**
                 * Direct reading of MixinNodes is not supported, since it is not
                 * possible in legacy APIs create pointer to Mixin Nodes.
                 *
                 */
                return null;
            } else if (node instanceof DataContainerNode<?>) {
                return fromDataContainerNode(builder, (DataContainerNode<?>) node);
            } else if (node instanceof AnyXmlNode) {
                return fromAnyXmlNode(builder, ((AnyXmlNode) node).getValue());
            }
            return fromNormalizedNode(builder, node);

        }


        private NormalizedNodeMessages.Node fromAnyXmlNode(NormalizedNodeMessages.Node.Builder builder, Node node){
            if(node instanceof CompositeNode){
                return fromCompositeNode(builder, (CompositeNode) node);
            } else {
                return fromSimpleNode(builder, (SimpleNode) node);
            }
        }

        private NormalizedNodeMessages.Node fromCompositeNode(
            NormalizedNodeMessages.Node.Builder builder,
            CompositeNode node) {
            if(builder == null) {
                builder =
                    NormalizedNodeMessages.Node.newBuilder();
            }

            setNodeType(builder, node.getNodeType());
            for (Object child : node.getValue()) {
                if(child instanceof CompositeNode){
                    builder.addChild(fromCompositeNode(null, (CompositeNode) child));
                } else {
                    builder.addChild(fromSimpleNode(null, (SimpleNode) child));
                }
            }
            return builder.build();
        }

        private NormalizedNodeMessages.Node fromSimpleNode(
            NormalizedNodeMessages.Node.Builder builder,
            SimpleNode child) {

            if(builder == null) {
                builder =
                    NormalizedNodeMessages.Node.newBuilder();
            }

            String nodeType = LeafNode.class.getSimpleName();

            Object value = child.getValue();

            if(value == null){
                value = "";
            }

            addNodeValueToBuilder(nodeType, value, child.getNodeType() , builder);

            return builder.build();
        }

        private NormalizedNodeMessages.Node fromNormalizedNode(
            NormalizedNodeMessages.Node.Builder builder,
            final NormalizedNode<?, ?> node) {
            if(builder == null) {
                builder =
                    NormalizedNodeMessages.Node.newBuilder();
            }
            buildLeafNode(node, builder);
            return builder.build();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private NormalizedNodeMessages.Node fromDataContainerNode(
            NormalizedNodeMessages.Node.Builder builder,
            final DataContainerNode<?> node) {
            if(builder == null){
                builder =
                    NormalizedNodeMessages.Node.newBuilder();
            }


            setNodeType(builder, node.getNodeType());

            builder.setType(DataContainerNode.class.getSimpleName());
            for (NormalizedNode<?, ?> child : node.getValue()) {
                if (child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                    builder.addAllChild(fromMixin((NormalizedNodeContainer) child));
                } else if (child instanceof UnkeyedListNode) {
                    builder.addAllChild(fromUnkeyedListNode((UnkeyedListNode) child));
                } else {
                    addToBuilder(builder, toNode(null, child));
                }
            }
            return builder.build();
        }

        private Iterable<NormalizedNodeMessages.Node> fromUnkeyedListNode(
            final UnkeyedListNode mixin) {
            ArrayList<NormalizedNodeMessages.Node> ret = new ArrayList<>();
            for (NormalizedNode<?, ?> child : mixin.getValue()) {
                ret.add(toNode(null, child));
            }
            return FluentIterable.from(ret).filter(Predicates.notNull());
        }

        private void addToBuilder(final NormalizedNodeMessages.Node.Builder builder, final NormalizedNodeMessages.Node legacy) {
            if (legacy != null) {
                builder.addChild(legacy);
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Iterable<NormalizedNodeMessages.Node> fromMixin(
            final NormalizedNodeContainer<?, ?, NormalizedNode<?, ?>> mixin) {
            ArrayList<NormalizedNodeMessages.Node> ret = new ArrayList<>();
            for (NormalizedNode<?, ?> child : mixin.getValue()) {
                if (child instanceof MixinNode && child instanceof NormalizedNodeContainer<?, ?, ?>) {
                    Iterables.addAll(ret,
                        fromMixin((NormalizedNodeContainer) child));
                } else {
                    ret.add(toNode(null, child));
                }
            }
            return FluentIterable.from(ret).filter(Predicates.notNull());
        }


        private void buildLeafNode(NormalizedNode<?, ?> normalizedNode,
            NormalizedNodeMessages.Node.Builder builder) {
            Map<QName, String> attributes;
            if(normalizedNode instanceof LeafNode){
                attributes = ((LeafNode) normalizedNode).getAttributes();
            } else {
                attributes = ((LeafSetEntryNode) normalizedNode).getAttributes();
            }
            buildAttributes(builder, attributes);
            buildNodeValue(normalizedNode, builder);
        }

        private void buildAttributes(
            NormalizedNodeMessages.Node.Builder builder,
            Map<QName, String> attributes) {
            if (!attributes.isEmpty()) {
                for (Map.Entry<QName, String> attribute : attributes.entrySet()) {
                    NormalizedNodeMessages.Attribute.Builder attributeBuilder
                        = NormalizedNodeMessages.Attribute.newBuilder();
                    attributeBuilder
                        .setName(attribute.getKey().toString())
                        .setValue(attribute.getValue().toString());

                    builder.addAttributes(attributeBuilder.build());
                }
            }
        }

        private void buildNodeValue(NormalizedNode<?, ?> normalizedNode,
            NormalizedNodeMessages.Node.Builder builder) {

            Object value = normalizedNode.getValue();
            String nodeType = LeafNode.class.getSimpleName();

            if(normalizedNode instanceof LeafSetEntryNode){
                nodeType = LeafSetEntryNode.class.getSimpleName();
            }

            if(value == null){
                value = "";
            }

            addNodeValueToBuilder(nodeType, value,
                normalizedNode.getNodeType(), builder);
        }

        private void addNodeValueToBuilder(final String nodeType, final Object value,
            final QName path, final NormalizedNodeMessages.Node.Builder builder){

            setNodeType(builder, path)
                .setType(nodeType)
                .setValueType((value.getClass().getSimpleName()))
                .setValue(value.toString());

            if (value.getClass().equals(YangInstanceIdentifier.class)) {
                builder.setInstanceIdentifierValue(
                    InstanceIdentifierUtils
                        .toSerializable((YangInstanceIdentifier) value)
                );
            }
        }

        private NormalizedNodeMessages.Node.Builder setNodeType(NormalizedNodeMessages.Node.Builder builder, QName nodeType){
            NormalizedNodeMessages.QName.Builder qName = NormalizedNodeMessages.QName.newBuilder();
            URI namespace = nodeType.getNamespace();
            Integer namespaceInt;

            if(namespaces.containsKey(namespace)){
                namespaceInt = namespaces.get(namespace);
            } else {
                namespaceInt = this.builder.getNamespaceCount();
                this.builder.addNamespace(namespace.toString());
                namespaces.put(namespace, namespaceInt);
            }

            qName.setNamespace(namespaceInt);

            Date revision = nodeType.getRevision();
            Integer revisionInt = -1;

            if(revisions.containsKey(revision)){
                revisionInt = revisions.get(revision);
            } else {
                if(nodeType.getRevision() != null) {
                    revisionInt = this.builder.getRevisionCount();
                    this.builder.addRevision(
                        SimpleDateFormatUtil.getRevisionFormat().format(
                            nodeType.getRevision())
                    );
                    revisions.put(revision, revisionInt);
                }
            }

            qName.setRevision(revisionInt);
            qName.setLocalName(nodeType.getLocalName());
            return builder.setNodeType(qName.build());
        }

    }

    public static class TopLevelNode {
        private final NormalizedNodeMessages.Node node;

        public TopLevelNode(NormalizedNodeMessages.Node node) {
            this.node = node;
        }

        public String toString(NormalizedNodeMessages.QName qName){
            Preconditions.checkArgument(!"".equals(qName.getLocalName()), "qName.localName cannot be empty");
            Preconditions.checkArgument(qName.getNamespace() != -1, "qName.namespace should be valid");

            StringBuilder sb = new StringBuilder();
            String namespace = node.getNamespace(qName.getNamespace());
            String revision = "";
            if(qName.getRevision() != -1){
                revision = node.getRevision(qName.getRevision());
            }


            sb.append("(").append(namespace).append("?revision=").append(
                revision).append(")").append(
                qName.getLocalName());
            return sb.toString();

        }
    }
}
