/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeSnapshot;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;

/**
 * A walking context, pretty much equivalent to an iterator, but it
 * exposes the underlying tree structure.
 *
 * @author Robert Varga
 */
public class ListenerWalker implements AutoCloseable {
    private final RegistrationTreeSnapshot<DataChangeListenerRegistration<?>> delegate;

    ListenerWalker(final RegistrationTreeSnapshot<DataChangeListenerRegistration<?>> delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    public ListenerNode getRootNode() {
        return new ListenerNode(delegate.getRootNode());
    }

    @Override
    public void close() {
        delegate.getRootNode();
    }
}