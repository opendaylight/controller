/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

import com.google.common.base.Optional;

public class TreeNodeUtils {

    /**
     * Finds a node in tree
     *
     * @param tree Data Tree
     * @param path Path to the node
     * @return Optional with node if the node is present in tree, {@link Optional#absent()} otherwise.
     *
     */
    public static <T extends StoreTreeNode<T>> Optional<T> findNode(final T tree, final InstanceIdentifier path) {
        Optional<T> current = Optional.<T> of(tree);
        Iterator<PathArgument> pathIter = path.getPath().iterator();
        while (current.isPresent() && pathIter.hasNext()) {
            current = current.get().getChild(pathIter.next());
        }
        return current;
    }

    /**
     * Finds a node or closest parent in  the tree
     *
     * @param tree Data Tree
     * @param path Path to the node
     * @return Map.Entry Entry with key which is path to closest parent and value is parent node.
     *
     */
    public static <T extends StoreTreeNode<T>> Map.Entry<InstanceIdentifier, T> findClosest(final T tree, final InstanceIdentifier path) {
        Optional<T> parent = Optional.<T>of(tree);
        Optional<T> current = Optional.<T> of(tree);

        int nesting = 0;
        Iterator<PathArgument> pathIter = path.getPath().iterator();
        while (current.isPresent() && pathIter.hasNext()) {
            parent = current;
            current = current.get().getChild(pathIter.next());
            nesting++;
        }
        if(current.isPresent()) {
            final InstanceIdentifier currentPath = new InstanceIdentifier(path.getPath().subList(0, nesting));
            return new SimpleEntry<InstanceIdentifier,T>(currentPath,current.get());
        }
        // Nesting minus one is safe, since current is allways present when nesting = 0
        // so this prat of code is never triggered, in cases nesting == 0;
        final InstanceIdentifier parentPath = new InstanceIdentifier(path.getPath().subList(0, nesting - 1));
        return new SimpleEntry<InstanceIdentifier,T>(parentPath,parent.get());
    }

    public static <T extends StoreTreeNode<T>> Optional<T> getChild(final Optional<T> parent,final PathArgument child) {
        if(parent.isPresent()) {
            return parent.get().getChild(child);
        }
        return Optional.absent();
    }

}
