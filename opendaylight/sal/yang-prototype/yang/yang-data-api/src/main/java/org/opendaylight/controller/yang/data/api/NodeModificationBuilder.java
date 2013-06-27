/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.api;


/**
 * @author michal.rehak
 *
 */
public interface NodeModificationBuilder {

    public abstract Node<?> getMutableEquivalent(Node<?> originalNode);

    public abstract CompositeNode buildDiffTree();

    public abstract void mergeNode(MutableCompositeNode alteredNode);

    public abstract void removeNode(MutableCompositeNode deadNode);

    public abstract void removeNode(MutableSimpleNode<?> deadNode);

    public abstract void deleteNode(MutableSimpleNode<?> deadNode);

    public abstract void deleteNode(MutableCompositeNode deadNode);

    public abstract void replaceNode(MutableCompositeNode replacementNode);

    public abstract void replaceNode(MutableSimpleNode<?> replacementNode);

    public abstract void addNode(MutableCompositeNode newNode);

    public abstract void addNode(MutableSimpleNode<?> newNode);

}
