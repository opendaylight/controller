/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.api;

/**
 * Simple node represents a leaf in the data tree, which does not contain any
 * nested nodes, but the value of node. In the terms of the XML the simple node
 * is element which contains only text data (CDATA or PCDATA). The simple node
 * is the manifestation of the following data schema constructs in YANG:
 * <ul>
 * <li><b>leaf</b> - simple node could represent YANG leafs of all types except
 * the empty type, which in XML form is similar to the empty container.</li>
 * <li><b>item</b> in <b>leaf-list</b></li>
 * </ul>
 * 
 * 
 * @param <T>
 */
public interface SimpleNode<T> extends Node<T> {

}
