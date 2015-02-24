/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

final class ShardedListenerRegistration<T extends DOMDataTreeListener> extends AbstractListenerRegistration<T> implements DOMDataTreeChangeListener {
    private volatile Queue<Collection<DataTreeCandidate>> queue = new LinkedList<>();

    protected ShardedListenerRegistration(final T listener) {
        super(listener);
    }

    @Override
    protected void removeRegistration() {
        // TODO Auto-generated method stub
    }

    private void dispatchChanges(final Collection<DataTreeCandidate> changes) {

        getInstance().onDataTreeChanged(Collections.singleton(c), subtrees);

    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        if (queue != null) {
            synchronized (this) {
                if (queue != null) {
                    queue.add(changes);
                    return;
                }
            }
        }

        // TODO Auto-generated method stub
    }

    synchronized void startForwarding(final Collection<ListenerRegistration<?>> regs) {
        final Queue<Collection<DataTreeCandidate>> q = queue;

        Preconditions.checkState(q != null);
        while (!q.isEmpty()) {
            final Collection<DataTreeCandidate> c = q.poll();
            dispatchChanges(c);
        }

        queue = null;
    }
}
