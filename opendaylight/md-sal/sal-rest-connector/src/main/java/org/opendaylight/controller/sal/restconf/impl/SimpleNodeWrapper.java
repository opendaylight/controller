package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

import com.google.common.base.Preconditions;

public final class SimpleNodeWrapper implements NodeWrapper<SimpleNode<?>>, SimpleNode<String> {
    
    private SimpleNode<String> simpleNode;
    
    private String localName;
    private String value;
    private URI namespace;

    public SimpleNodeWrapper(String localName, String value) {
        this.localName = Preconditions.checkNotNull(localName);
        this.value = value;
    }
    
    public SimpleNodeWrapper(URI namespace, String localName, String value) {
        this(localName, value);
        this.namespace = namespace;
    }
    
    @Override
    public String getLocalName() {
        if (simpleNode != null) {
            return simpleNode.getNodeType().getLocalName();
        }
        return localName;
    }
    
    @Override
    public URI getNamespace() {
        if (simpleNode != null) {
            return simpleNode.getNodeType().getNamespace();
        }
        return namespace;
    }

    @Override
    public void setNamespace(URI namespace) {
        Preconditions.checkState(simpleNode == null, "Cannot change the object, due to data inconsistencies.");
        this.namespace = namespace;
    }

    @Override
    public SimpleNode<String> unwrap(CompositeNode parent) {
        if (simpleNode == null) {
            Preconditions.checkNotNull(namespace);
            simpleNode = NodeFactory.createImmutableSimpleNode(new QName(namespace, localName), parent, value);
            
            value = null;
            namespace = null;
            localName = null;
        }
        return simpleNode;
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
    public String getValue() {
        return unwrap(null).getValue();
    }

    @Override
    public ModifyAction getModificationAction() {
        return unwrap(null).getModificationAction();
    }

    @Override
    public MutableSimpleNode<String> asMutable() {
        return unwrap(null).asMutable();
    }

    @Override
    public QName getKey() {
        return unwrap(null).getKey();
    }

    @Override
    public String setValue(String value) {
        return unwrap(null).setValue(value);
    }


}
