/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;

/**
 * @author michal.rehak
 * @param <T>
 *            type of node value
 * 
 */
public abstract class AbstractNodeTO<T> implements Node<T> {

    private QName qName;
    private CompositeNode parent;
    private T value;

    /**
     * @param qname
     * @param parent
     * @param value
     */
    public AbstractNodeTO(QName qname, CompositeNode parent, T value) {
        this.qName = qname;
        this.parent = parent;
        this.value = value;
    }

    @Override
    public QName getNodeType() {
        return qName;
    }

    /**
     * @return the qName
     */
    protected QName getQName() {
        return qName;
    }

    @Override
    public CompositeNode getParent() {
        return parent;
    }
    
    /**
     * @param parent the parent to set
     */
    public void setParent(CompositeNode parent) {
        this.parent = parent;
    }
    
    /**
     * @param value the value to set
     */
    protected void setValue(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }
}
