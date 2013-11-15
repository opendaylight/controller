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
    private List<NodeWrapper<?>> values = new ArrayList<>();
    
    public CompositeNodeWrapper(String localName) {
        this.localName = Preconditions.checkNotNull(localName);
    }
    
    public CompositeNodeWrapper(URI namespace, String localName) {
        this(localName);
        this.namespace = namespace;
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
    public CompositeNode unwrap(CompositeNode parent) {
        if (compositeNode == null) {
            Preconditions.checkNotNull(namespace);
            compositeNode = NodeFactory.createMutableCompositeNode(new QName(namespace, localName), 
                    parent, new ArrayList<Node<?>>(), ModifyAction.CREATE, null);
            
            List<Node<?>> nodeValues = new ArrayList<>();
            for (NodeWrapper<?> nodeWrapper : values) {
                nodeValues.add(nodeWrapper.unwrap(compositeNode));
            }
            compositeNode.setValue(nodeValues);
            
            values = null;
            namespace = null;
            localName = null;
        }
        return compositeNode;
    }

    @Override
    public QName getNodeType() {
        return unwrap(null).getNodeType();
    }

    @Override
    public CompositeNode getParent() {
        return unwrap(null).getParent();
    }

    @Override
    public List<Node<?>> getValue() {
        return unwrap(null).getValue();
    }

    @Override
    public ModifyAction getModificationAction() {
        return unwrap(null).getModificationAction();
    }

    @Override
    public List<Node<?>> getChildren() {
        return unwrap(null).getChildren();
    }

    @Override
    public List<CompositeNode> getCompositesByName(QName children) {
        return unwrap(null).getCompositesByName(children);
    }

    @Override
    public List<CompositeNode> getCompositesByName(String children) {
        return unwrap(null).getCompositesByName(children);
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
        return unwrap(null).getSimpleNodesByName(children);
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(String children) {
        return unwrap(null).getSimpleNodesByName(children);
    }

    @Override
    public CompositeNode getFirstCompositeByName(QName container) {
        return unwrap(null).getFirstCompositeByName(container);
    }

    @Override
    public SimpleNode<?> getFirstSimpleByName(QName leaf) {
        return unwrap(null).getFirstSimpleByName(leaf);
    }

    @Override
    public MutableCompositeNode asMutable() {
        return unwrap(null).asMutable();
    }

    @Override
    public QName getKey() {
        return unwrap(null).getKey();
    }

    @Override
    public List<Node<?>> setValue(List<Node<?>> value) {
        return unwrap(null).setValue(value);
    }

    @Override
    public int size() {
        return unwrap(null).size();
    }

    @Override
    public boolean isEmpty() {
        return unwrap(null).isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return unwrap(null).containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return unwrap(null).containsValue(value);
    }

    @Override
    public List<Node<?>> get(Object key) {
        return unwrap(null).get(key);
    }

    @Override
    public List<Node<?>> put(QName key, List<Node<?>> value) {
        return unwrap(null).put(key, value);
    }

    @Override
    public List<Node<?>> remove(Object key) {
        return unwrap(null).remove(key);
    }

    @Override
    public void putAll(Map<? extends QName, ? extends List<Node<?>>> m) {
        unwrap(null).putAll(m);
    }

    @Override
    public void clear() {
        unwrap(null).clear();
    }

    @Override
    public Set<QName> keySet() {
        return unwrap(null).keySet();
    }

    @Override
    public Collection<List<Node<?>>> values() {
        return unwrap(null).values();
    }

    @Override
    public Set<java.util.Map.Entry<QName, List<Node<?>>>> entrySet() {
        return unwrap(null).entrySet();
    }

}
