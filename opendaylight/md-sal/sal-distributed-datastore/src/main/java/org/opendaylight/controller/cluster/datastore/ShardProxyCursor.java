/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class ShardProxyCursor implements DOMDataTreeWriteCursor {

    private final YangInstanceIdentifier root;
    private final Deque<PathArgument> stack = new ArrayDeque<>();
    private final ClientTransaction tx;

    ShardProxyCursor(final DOMDataTreeIdentifier prefix, final ClientTransaction tx) {
        //TODO migrate whole package to mdsal LogicalDatastoreType
        root = prefix.getRootIdentifier();

        this.tx = tx;
    }

    @Override
    public void delete(final PathArgument child) {
        tx.delete(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))));
    }

    @Override
    public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
        tx.merge(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))), data);
    }

    @Override
    public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
        tx.write(YangInstanceIdentifier.create(Iterables.concat(root.getPathArguments(), stack, Collections.singletonList(child))), data);
    }

    @Override
    public void enter(@Nonnull final PathArgument child) {
        stack.push(child);
    }

    @Override
    public void enter(@Nonnull final PathArgument... path) {
        for (final PathArgument pathArgument : path) {
            enter(pathArgument);
        }
    }

    @Override
    public void enter(@Nonnull final Iterable<PathArgument> path) {
        path.forEach(this::enter);
    }

    @Override
    public void exit() {
        stack.pop();
    }

    @Override
    public void exit(final int depth) {
        for (int i = 0; i < depth; i++) {
            exit();
        }
    }

    @Override
    public void close() {

    }
}
