/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext
 *
 */
public class NormalizedNodePruner implements NormalizedNodeStreamWriter {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodePruner.class);

    public static final URI BASE_NAMESPACE = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    private final SimpleStack<NormalizedNodeBuilderWrapper> stack = new SimpleStack<>();
    private NormalizedNode<?,?> normalizedNode;
    private final DataSchemaContextNode<?> nodePathSchemaNode;
    private boolean sealed = false;

    public NormalizedNodePruner(YangInstanceIdentifier nodePath, SchemaContext schemaContext) {
        nodePathSchemaNode = findSchemaNodeForNodePath(nodePath, schemaContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void leafNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, Object o) throws IOException, IllegalArgumentException {

        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(nodeIdentifier).withValue(o).build();
        if(parent != null) {
            if(hasValidSchema(nodeIdentifier.getNodeType(), parent)) {
                parent.builder().addChild(leafNode);
            }
        } else {
            // If there's no parent node then this is a stand alone LeafNode.
            if(nodePathSchemaNode != null) {
                this.normalizedNode = leafNode;
            }

            sealed = true;
        }
    }

    @Override
    public void startLeafSet(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startOrderedLeafSet(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.orderedLeafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void leafSetEntryNode(QName name, Object o) throws IOException, IllegalArgumentException {
        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        if(parent != null) {
            if(hasValidSchema(name, parent)) {
                parent.builder().addChild(Builders.leafSetEntryBuilder().withValue(o).withNodeIdentifier(
                        new YangInstanceIdentifier.NodeWithValue<>(parent.nodeType(), o)).build());
            }
        } else {
            // If there's no parent LeafSetNode then this is a stand alone LeafSetEntryNode.
            if(nodePathSchemaNode != null) {
                this.normalizedNode = Builders.leafSetEntryBuilder().withValue(o).withNodeIdentifier(
                        new YangInstanceIdentifier.NodeWithValue<>(name, o)).build();
            }

            sealed = true;
        }
    }

    @Override
    public void startContainerNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.containerBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startYangModeledAnyXmlNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.unkeyedListBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalStateException {

        checkNotSealed();

        addBuilder(Builders.unkeyedListEntryBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.mapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifierWithPredicates, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifierWithPredicates), nodeIdentifierWithPredicates);
    }

    @Override
    public void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.orderedMapBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startChoiceNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.choiceBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.augmentationBuilder().withNodeIdentifier(augmentationIdentifier), augmentationIdentifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void anyxmlNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, Object o) throws IOException, IllegalArgumentException {
        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        AnyXmlNode anyXmlNode = Builders.anyXmlBuilder().withNodeIdentifier(nodeIdentifier).
                withValue((DOMSource) o).build();
        if(parent != null) {
            if(hasValidSchema(nodeIdentifier.getNodeType(), parent)) {
                parent.builder().addChild(anyXmlNode);
            }
        } else {
            // If there's no parent node then this is a stand alone AnyXmlNode.
            if(nodePathSchemaNode != null) {
                this.normalizedNode = anyXmlNode;
            }

            sealed = true;
        }
    }

    @Override
    public void endNode() throws IOException, IllegalStateException {

        checkNotSealed();

        NormalizedNodeBuilderWrapper child = stack.pop();

        Preconditions.checkState(child != null, "endNode called on an empty stack");

        if(!child.getSchema().isPresent()) {
            LOG.debug("Schema not found for {}", child.identifier());
            return;
        }

        NormalizedNode<?,?> normalizedNode = child.builder().build();

        if(stack.size() > 0) {
            NormalizedNodeBuilderWrapper parent = stack.peek();
            parent.builder().addChild(normalizedNode);
        } else {
            this.normalizedNode = normalizedNode;
            sealed = true;
        }
    }

    @Override
    public void close() throws IOException {
        sealed = true;
    }

    @Override
    public void flush() throws IOException {

    }

    public NormalizedNode<?,?> normalizedNode(){
        return normalizedNode;
    }

    private void checkNotSealed(){
        Preconditions.checkState(!sealed, "Pruner can be used only once");
    }

    private boolean hasValidSchema(QName name, NormalizedNodeBuilderWrapper parent) {
        boolean valid = parent.getSchema().isPresent() && parent.getSchema().get().getChild(name) != null;
        if(!valid) {
            LOG.debug("Schema not found for {}", name);
        }

        return valid;
    }

    private NormalizedNodeBuilderWrapper addBuilder(NormalizedNodeContainerBuilder<?,?,?,?> builder,
            PathArgument identifier){
        final Optional<DataSchemaContextNode<?>> schemaNode;
        NormalizedNodeBuilderWrapper parent = stack.peek();
        if(parent == null) {
            schemaNode = Optional.fromNullable(nodePathSchemaNode);
        } else if(parent.getSchema().isPresent()) {
            schemaNode = Optional.fromNullable(parent.getSchema().get().getChild(identifier));
        } else {
            schemaNode = Optional.absent();
        }

        NormalizedNodeBuilderWrapper wrapper = new NormalizedNodeBuilderWrapper(builder, identifier, schemaNode);
        stack.push(wrapper);
        return wrapper;
    }

    private static DataSchemaContextNode<?> findSchemaNodeForNodePath(YangInstanceIdentifier nodePath,
            SchemaContext schemaContext) {
        DataSchemaContextNode<?> schemaNode = DataSchemaContextTree.from(schemaContext).getRoot();
        for(PathArgument arg : nodePath.getPathArguments()) {
            schemaNode = schemaNode.getChild(arg);
            if(schemaNode == null) {
                break;
            }
        }

        return schemaNode;
    }

    @VisibleForTesting
    static class SimpleStack<E> {
        List<E> stack = new LinkedList<>();

        void push(E element){
            stack.add(element);
        }

        E pop(){
            if(size() == 0){
                return null;
            }
            return stack.remove(stack.size() - 1);
        }

        E peek(){
            if(size() == 0){
                return null;
            }

            return stack.get(stack.size() - 1);
        }

        int size(){
            return stack.size();
        }
    }
}
