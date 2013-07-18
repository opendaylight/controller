/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.api;

import java.util.List;


/**
 * @author michal.rehak
 *
 */
public interface MutableCompositeNode extends MutableNode<List<Node<?>>>, CompositeNode {
    
    /**
     * update internal map
     */
    public void init();
    
    /**
     * @return original node, if available
     */
    CompositeNode getOriginal();
}
