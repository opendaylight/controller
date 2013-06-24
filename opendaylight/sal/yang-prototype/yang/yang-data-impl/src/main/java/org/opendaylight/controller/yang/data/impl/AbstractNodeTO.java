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
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;

/**
 * @author michal.rehak
 * @param <T>
 *            type of node value
 * 
 */
public abstract class AbstractNodeTO<T> implements Node<T>, NodeModification {

    private QName qName;
    private CompositeNode parent;
    private T value;
    private ModifyAction modifyAction;
    
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
    
    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction 
     */
    public AbstractNodeTO(QName qname, CompositeNode parent, T value, ModifyAction modifyAction) {
        this.qName = qname;
        this.parent = parent;
        this.value = value;
        this.modifyAction = modifyAction;
    }

    @Override
    public QName getNodeType() {
        return qName;
    }

    /**
     * @return the qName
     */
    public QName getQName() {
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

    /**
     * @return modification action
     * @see org.opendaylight.controller.yang.data.impl.NodeModificationSupport#getModificationAction()
     */
    @Override
    public ModifyAction getModificationAction() {
        return modifyAction;
    }

    /**
     * @param modifyAction
     *            the modifyAction to set
     */
    protected void setModificationAction(ModifyAction modifyAction) {
        this.modifyAction = modifyAction;
    }
    
    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append(String.format("Node[%s], qName[%s], modify[%s]", 
                getClass().getSimpleName(), getQName().getLocalName(),
                getModificationAction() == null ? "n/a" : getModificationAction()));
        return out.toString();
    }

    /* */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((modifyAction == null) ? 0 : modifyAction.hashCode());
//        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((qName == null) ? 0 : qName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result % 2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        AbstractNodeTO<T> other = (AbstractNodeTO<T>) obj;
        if (modifyAction != other.modifyAction)
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (other.parent == null) {
            return false;
        } 
        if (qName == null) {
            if (other.qName != null)
                return false;
        } else if (!qName.equals(other.qName))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
    /* */
    
}
