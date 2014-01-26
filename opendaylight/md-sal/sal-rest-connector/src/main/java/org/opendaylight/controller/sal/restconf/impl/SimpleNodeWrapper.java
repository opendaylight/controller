/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

import com.google.common.base.Preconditions;

public final class SimpleNodeWrapper implements NodeWrapper<SimpleNode<?>>, SimpleNode<Object> {
    
    private SimpleNode<Object> simpleNode;
    
    private String localName;
    private Object value;
    private URI namespace;
    private QName name;

    public SimpleNodeWrapper(String localName, Object value) {
        this.localName = Preconditions.checkNotNull(localName);
        this.value = value;
    }
    
    public SimpleNodeWrapper(URI namespace, String localName, Object value) {
        this(localName, value);
        this.namespace = namespace;
    }
    
    @Override
    public void setQname(QName name) {
        Preconditions.checkState(simpleNode == null, "Cannot change the object, due to data inconsistencies.");
        this.name = name;
    }
    
    @Override
    public QName getQname() {
        return name;
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
    public boolean isChangeAllowed() {
        return simpleNode == null ? true : false;
    }

    @Override
    public SimpleNode<Object> unwrap() {
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
        return (SimpleNode<Object>) simpleNode;
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
    public Object getValue() {
        return unwrap().getValue();
    }

    @Override
    public ModifyAction getModificationAction() {
        return unwrap().getModificationAction();
    }

    @Override
    public MutableSimpleNode<Object> asMutable() {
        return unwrap().asMutable();
    }

    @Override
    public QName getKey() {
        return unwrap().getKey();
    }

    @Override
    public Object setValue(Object value) {
        return unwrap().setValue(value);
    }
    


}
