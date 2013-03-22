/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.api;

import java.util.List;

import org.opendaylight.controller.yang.common.QName;

/**
 * Composite node represents a branch in the data tree, which could contain
 * nested composite nodes or leaf nodes. In the terms of the XML the simple node
 * is element which does not text data directly (CDATA or PCDATA), only other
 * nodes. The composite node is the manifestation of the following data schema
 * constructs in the YANG:
 * 
 * <ul>
 * <li><b>container</b> - the composite node represents the YANG container and
 * could contain all children schema nodes of that container</li>
 * <li><b>item</b> in the <b>list</b> - the composite node represents one item
 * in the YANG list and could contain all children schema nodes of that list
 * item.</li>
 * <li><b>anyxml</b></li>
 * </ul>
 * 
 * 
 */
public interface CompositeNode extends Node<List<Node<?>>> {

    List<Node<?>> getChildren();

    List<CompositeNode> getCompositesByName(QName children);

    List<CompositeNode> getCompositesByName(String children);

    List<SimpleNode<?>> getSimpleNodesByName(QName children);

    List<SimpleNode<?>> getSimpleNodesByName(String children);

    CompositeNode getFirstCompositeByName(QName container);

    SimpleNode<?> getFirstSimpleByName(QName leaf);

}
