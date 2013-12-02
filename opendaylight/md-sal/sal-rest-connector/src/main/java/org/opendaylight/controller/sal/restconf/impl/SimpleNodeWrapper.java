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
    private QName name;

    public SimpleNodeWrapper(String localName, String value) {
        this.localName = Preconditions.checkNotNull(localName);
        this.value = value;
    }
    
    public SimpleNodeWrapper(URI namespace, String localName, String value) {
        this(localName, value);
        this.namespace = namespace;
    }
    
    @Override
    public void setQname(QName name) {
        Preconditions.checkState(simpleNode == null, "Cannot change the object, due to data inconsistencies.");
        this.name = name;
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
    public SimpleNode<String> unwrap() {
        if (simpleNode == null) {
            if (name == null) {
                Preconditions.checkNotNull(namespace);
                name = new QName(namespace, localName);
            }
            simpleNode = NodeFactory.createImmutableSimpleNode(name, null, value);
            
            value = null;
            namespace = null;
            localName = null;
            name = null;
        }
        return simpleNode;
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
    public String getValue() {
        return unwrap().getValue();
    }

    @Override
    public ModifyAction getModificationAction() {
        return unwrap().getModificationAction();
    }

    @Override
    public MutableSimpleNode<String> asMutable() {
        return unwrap().asMutable();
    }

    @Override
    public QName getKey() {
        return unwrap().getKey();
    }

    @Override
    public String setValue(String value) {
        return unwrap().setValue(value);
    }


}
