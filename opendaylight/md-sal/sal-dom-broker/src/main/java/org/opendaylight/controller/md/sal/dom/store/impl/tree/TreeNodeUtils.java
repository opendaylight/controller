/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

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


    public static <T extends StoreTreeNode<T>> T findNodeChecked(final T tree, final InstanceIdentifier path) {
        T current = tree;
        List<PathArgument> nested = new ArrayList<>(path.getPath().size());
        for(PathArgument pathArg : path.getPath()) {
            Optional<T> potential = current.getChild(pathArg);
            nested.add(pathArg);
            Preconditions.checkArgument(potential.isPresent(),"Child %s is not present in tree.",nested);
            current = potential.get();
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
        return findClosestsOrFirstMatch(tree, path, Predicates.<T>alwaysFalse());
    }

    public static <T extends StoreTreeNode<T>> Map.Entry<InstanceIdentifier, T> findClosestsOrFirstMatch(final T tree, final InstanceIdentifier path, final Predicate<T> predicate) {
        Optional<T> parent = Optional.<T>of(tree);
        Optional<T> current = Optional.<T> of(tree);

        int nesting = 0;
        Iterator<PathArgument> pathIter = path.getPath().iterator();
        while (current.isPresent() && pathIter.hasNext() && !predicate.apply(current.get())) {
            parent = current;
            current = current.get().getChild(pathIter.next());
            nesting++;
        }
        if(current.isPresent()) {
            final InstanceIdentifier currentPath = new InstanceIdentifier(path.getPath().subList(0, nesting));
            return new SimpleEntry<InstanceIdentifier,T>(currentPath,current.get());
        }

        /*
         * Subtracting 1 from nesting level at this point is safe, because we
         * cannot reach here with nesting == 0: that would mean the above check
         * for current.isPresent() failed, which it cannot, as current is always
         * present. At any rate we check state just to be on the safe side.
         */
        Preconditions.checkState(nesting > 0);
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
