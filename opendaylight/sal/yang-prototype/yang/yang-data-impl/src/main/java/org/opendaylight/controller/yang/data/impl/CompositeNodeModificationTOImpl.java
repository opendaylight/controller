/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.Node;

/**
 * @author michal.rehak
 * 
 */
public class CompositeNodeModificationTOImpl extends CompositeNodeTOImpl {

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public CompositeNodeModificationTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value, ModifyAction modifyAction) {
        super(qname, parent, value);
        super.setModificationAction(modifyAction);
    }
}
