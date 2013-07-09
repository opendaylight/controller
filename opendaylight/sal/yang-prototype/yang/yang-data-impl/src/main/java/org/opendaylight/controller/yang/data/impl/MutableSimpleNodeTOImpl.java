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

/**
 * @author michal.rehak
 * @param <T> type of simple node value
 * 
 */
public class MutableSimpleNodeTOImpl<T> extends SimpleNodeModificationTOImpl<T> 
        implements MutableSimpleNode<T> {

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public MutableSimpleNodeTOImpl(QName qname, CompositeNode parent, T value,
            ModifyAction modifyAction) {
        super(qname, parent, value, modifyAction);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
    
    @Override
    public void setModifyAction(ModifyAction action) {
        super.setModificationAction(action);
    }
}
