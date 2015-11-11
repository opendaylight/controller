/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.dom.DOMSource;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext
 *
 */
public class NormalizedNodePruner implements NormalizedNodeStreamWriter {

    public static final URI BASE_NAMESPACE = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    private final SimpleStack<NormalizedNodeBuilderWrapper> stack = new SimpleStack<>();
    private NormalizedNode<?,?> normalizedNode;
    private final Set<URI> validNamespaces;
    private boolean sealed = false;

    public NormalizedNodePruner(SchemaContext schemaContext) {
        this(NormalizedNodePruner.namespaces(schemaContext));
    }

    public NormalizedNodePruner(Set<URI> validNamespaces) {
        this.validNamespaces = validNamespaces;
    }

    @Override
    public void leafNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, Object o) throws IOException, IllegalArgumentException {

        checkNotSealed();

        if(!isValidNamespace(nodeIdentifier)){
            return;
        }
        NormalizedNodeBuilderWrapper parent = stack.peek();
        Preconditions.checkState(parent != null, "leafNode has no parent");
        parent.builder()
                .addChild(Builders.leafBuilder()
                        .withNodeIdentifier(nodeIdentifier)
                        .withValue(o)
                        .build());
    }

    @Override
    public void startLeafSet(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, int i) throws IOException, IllegalArgumentException {

        checkNotSealed();

        addBuilder(Builders.leafSetBuilder().withNodeIdentifier(nodeIdentifier), nodeIdentifier);
    }

    @Override
    public void leafSetEntryNode(Object o) throws IOException, IllegalArgumentException {

        checkNotSealed();

        NormalizedNodeBuilderWrapper parent = stack.peek();
        Preconditions.checkState(parent != null, "leafSetEntryNode has no parent");
        if(!isValidNamespace(parent.identifier())){
            return;
        }

        parent.builder()
                .addChild(Builders.leafSetEntryBuilder()
                        .withValue(o)
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue(parent.nodeType(), o))
                        .build());
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

    @Override
    public void anyxmlNode(YangInstanceIdentifier.NodeIdentifier nodeIdentifier, Object o) throws IOException, IllegalArgumentException {

        checkNotSealed();

        if(!isValidNamespace(nodeIdentifier)){
            return;
        }
        NormalizedNodeBuilderWrapper parent = stack.peek();
        Preconditions.checkState(parent != null, "anyxmlNode has no parent");
        parent.builder().addChild(Builders.anyXmlBuilder().withNodeIdentifier(nodeIdentifier).withValue((DOMSource) o).build());
    }

    @Override
    public void endNode() throws IOException, IllegalStateException {

        checkNotSealed();

        NormalizedNodeBuilderWrapper child = stack.pop();

        Preconditions.checkState(child != null, "endNode called on an empty stack");

        if(!isValidNamespace(child.identifier())){
            return;
        }
        NormalizedNode<?,?> normalizedNode = child.builder().build();

        if(stack.size() > 0){
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

    private boolean isValidNamespace(QName qName){
        return validNamespaces.contains(qName.getNamespace());
    }

    private boolean isValidNamespace(YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier){
        Set<QName> possibleChildNames = augmentationIdentifier.getPossibleChildNames();

        for(QName qName : possibleChildNames){
            if(isValidNamespace(qName)){
                return true;
            }
        }
        return false;

    }

    private boolean isValidNamespace(YangInstanceIdentifier.PathArgument identifier){
        if(identifier instanceof YangInstanceIdentifier.AugmentationIdentifier){
            return isValidNamespace((YangInstanceIdentifier.AugmentationIdentifier) identifier);
        }

        return isValidNamespace(identifier.getNodeType());
    }

    private NormalizedNodeBuilderWrapper addBuilder(NormalizedNodeContainerBuilder<?,?,?,?> builder, YangInstanceIdentifier.PathArgument identifier){
        NormalizedNodeBuilderWrapper wrapper = new NormalizedNodeBuilderWrapper(builder, identifier);
        stack.push(wrapper);
        return wrapper;
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

    @VisibleForTesting
    SimpleStack<NormalizedNodeBuilderWrapper> stack(){
        return stack;
    }

    public static Set<URI> namespaces(SchemaContext schemaContext){
        Set<URI> namespaces = new HashSet<>(schemaContext.getModules().size());
        namespaces.add(BASE_NAMESPACE);
        for(org.opendaylight.yangtools.yang.model.api.Module module : schemaContext.getModules()){
            namespaces.add(module.getNamespace());
        }
        return namespaces;
    }
}
