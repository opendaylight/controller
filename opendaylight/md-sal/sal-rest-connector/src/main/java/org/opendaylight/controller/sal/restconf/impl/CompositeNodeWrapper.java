/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

import com.google.common.base.Preconditions;

public final class CompositeNodeWrapper implements NodeWrapper<CompositeNode>, CompositeNode {

    private MutableCompositeNode compositeNode;

    private String localName;
    private URI namespace;
    private QName name;
    private List<NodeWrapper<?>> values = new ArrayList<>();

    public CompositeNodeWrapper(String localName) {
        this.localName = Preconditions.checkNotNull(localName);
    }

    public CompositeNodeWrapper(URI namespace, String localName) {
        this(localName);
        this.namespace = namespace;
    }
    
    @Override
    public void setQname(QName name) {
        Preconditions.checkState(compositeNode == null, "Cannot change the object, due to data inconsistencies.");
        this.name = name;
    }
    
    @Override
    public QName getQname() {
        return name;
    }

    @Override
    public String getLocalName() {
        if (compositeNode != null) {
            return compositeNode.getNodeType().getLocalName();
        }
        return localName;
    }

    @Override
    public URI getNamespace() {
        if (compositeNode != null) {
            return compositeNode.getNodeType().getNamespace();
        }
        return namespace;
    }

    @Override
    public void setNamespace(URI namespace) {
        Preconditions.checkState(compositeNode == null, "Cannot change the object, due to data inconsistencies.");
        this.namespace = namespace;
    }

    public void addValue(NodeWrapper<?> value) {
        Preconditions.checkState(compositeNode == null, "Cannot change the object, due to data inconsistencies.");
        values.add(value);
    }

    public void removeValue(NodeWrapper<CompositeNode> value) {
        Preconditions.checkState(compositeNode == null, "Cannot change the object, due to data inconsistencies.");
        values.remove(value);
    }
    
    public List<NodeWrapper<?>> getValues() {
        Preconditions.checkState(compositeNode == null, "Data can be inconsistent.");
        return Collections.unmodifiableList(values);
    }
    
    @Override
    public boolean isChangeAllowed() {
        return compositeNode == null ? true : false;
    }

    @Override
    public CompositeNode unwrap() {
        if (compositeNode == null) {
            if (name == null) {
                Preconditions.checkNotNull(namespace);
                name = new QName(namespace, localName);
            }
            
            List<Node<?>> nodeValues = new ArrayList<>();
            for (NodeWrapper<?> nodeWrapper : values) {
                nodeValues.add(nodeWrapper.unwrap());
            }
            compositeNode = NodeFactory.createMutableCompositeNode(name, null, nodeValues, null, null);
            
            values = null;
            namespace = null;
            localName = null;
            name = null;
        }
        return compositeNode;
    }

    @Override
    public QName getNodeType() {
        return unwrap().getNodeType();
    }

    @Override
    public CompositeNode getParent() {
        return unwrap().getParent();
    }

    @Override
    public List<Node<?>> getValue() {
        return unwrap().getValue();
    }

    @Override
    public ModifyAction getModificationAction() {
        return unwrap().getModificationAction();
    }

    @Override
    public List<Node<?>> getChildren() {
        return unwrap().getChildren();
    }

    @Override
    public List<CompositeNode> getCompositesByName(QName children) {
        return unwrap().getCompositesByName(children);
    }

    @Override
    public List<CompositeNode> getCompositesByName(String children) {
        return unwrap().getCompositesByName(children);
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
        return unwrap().getSimpleNodesByName(children);
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(String children) {
        return unwrap().getSimpleNodesByName(children);
    }

    @Override
    public CompositeNode getFirstCompositeByName(QName container) {
        return unwrap().getFirstCompositeByName(container);
    }

    @Override
    public SimpleNode<?> getFirstSimpleByName(QName leaf) {
        return unwrap().getFirstSimpleByName(leaf);
    }

    @Override
    public MutableCompositeNode asMutable() {
        return unwrap().asMutable();
    }

    @Override
    public QName getKey() {
        return unwrap().getKey();
    }

    @Override
    public List<Node<?>> setValue(List<Node<?>> value) {
        return unwrap().setValue(value);
    }

    @Override
    public int size() {
        return unwrap().size();
    }

    @Override
    public boolean isEmpty() {
        return unwrap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return unwrap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return unwrap().containsValue(value);
    }

    @Override
    public List<Node<?>> get(Object key) {
        return unwrap().get(key);
    }

    @Override
    public List<Node<?>> put(QName key, List<Node<?>> value) {
        return unwrap().put(key, value);
    }

    @Override
    public List<Node<?>> remove(Object key) {
        return unwrap().remove(key);
    }

    @Override
    public void putAll(Map<? extends QName, ? extends List<Node<?>>> m) {
        unwrap().putAll(m);
    }

    @Override
    public void clear() {
        unwrap().clear();
    }

    @Override
    public Set<QName> keySet() {
        return unwrap().keySet();
    }

    @Override
    public Collection<List<Node<?>>> values() {
        return unwrap().values();
    }

    @Override
    public Set<java.util.Map.Entry<QName, List<Node<?>>>> entrySet() {
        return unwrap().entrySet();
    }

}
