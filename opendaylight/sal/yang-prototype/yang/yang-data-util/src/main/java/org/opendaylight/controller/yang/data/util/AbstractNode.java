/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.util;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.Node;

public abstract class AbstractNode<T> implements Node<T> {

    private final QName nodeName;
    private final CompositeNode parent;

    protected AbstractNode(QName name, CompositeNode parent) {
        nodeName = name;
        this.parent = parent;
    }

    public QName getNodeType() {
        return this.nodeName;
    }

    public CompositeNode getParent() {
        return parent;
    }
    
    /* (non-Javadoc)
     */
    /**
     * @see org.opendaylight.controller.yang.data.api.NodeModification#getModificationAction()
     */
    public ModifyAction getModificationAction() {
        // TODO Auto-generated method stub
        return null;
    }
}
