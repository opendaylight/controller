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
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 * @param <T> type of simple node value
 * 
 */
public class SimpleNodeTOImpl<T> extends AbstractNodeTO<T> implements
        SimpleNode<T> {

    /**
     * @param qname
     * @param parent
     * @param value
     */
    public SimpleNodeTOImpl(QName qname, CompositeNode parent, T value) {
        super(qname, parent, value);
    }
    
    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction 
     */
    public SimpleNodeTOImpl(QName qname, CompositeNode parent, T value, ModifyAction modifyAction) {
        super(qname, parent, value, modifyAction);
    }

    
    @Override
    public MutableSimpleNode<T> asMutable() {
        throw new IllegalAccessError("cast to mutable is not supported - "+getClass().getSimpleName());
    }
    
    @Override
    public String toString() {
        return super.toString() + ", value = "+getValue();
    }
}
