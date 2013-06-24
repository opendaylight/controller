/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;

/**
 * @author michal.rehak
 * 
 */
public class MutableCompositeNodeTOImpl extends CompositeNodeTOImpl
        implements MutableCompositeNode {

    private Map<QName, List<Node<?>>> nodeMap;
    private CompositeNode original;

    /**
     * @param qname
     * @param parent
     * @param value
     * @param modifyAction
     */
    public MutableCompositeNodeTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value, ModifyAction modifyAction) {
        super(qname, parent, value, modifyAction);
    }
    
    /**
     * update nodeMap
     */
    @Override
    public void init() {
        if (!getChildren().isEmpty()) {
            nodeMap = NodeUtils.buildNodeMap(getChildren());
        }
    }

    @Override
    public void setValue(List<Node<?>> value) {
        super.setValue(value);
    }
    
    @Override
    public void setModifyAction(ModifyAction action) {
        super.setModificationAction(action);
    }
    
    @Override
    protected Map<QName, List<Node<?>>> getNodeMap() {
        return nodeMap;
    }
    
    @Override
    public MutableCompositeNode asMutable() {
        return this;
    }
    
    @Override
    public CompositeNode getOriginal() {
        return original;
    }
    
    /**
     * @param original the original to set
     */
    public void setOriginal(CompositeNode original) {
        this.original = original;
    }
}
