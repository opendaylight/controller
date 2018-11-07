/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

@Deprecated
public class DOMDataTreeChangeServiceAdapter extends ForwardingObject
        implements org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService {

    private final DOMDataTreeChangeService delegate;

    DOMDataTreeChangeServiceAdapter(final DOMDataTreeChangeService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <L extends org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> ListenerRegistration<L>
            registerDataTreeChangeListener(final org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier treeId,
                    final L listener) {
        final DOMDataTreeChangeListener delegateListener;
        if (listener instanceof org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener) {
            delegateListener = (ClusteredDOMDataTreeChangeListener) listener::onDataTreeChanged;
        } else {
            delegateListener = listener::onDataTreeChanged;
        }
        final ListenerRegistration<?> reg = delegate().registerDataTreeChangeListener(
            DOMDataTreeIdentifier.fromMdsal(treeId), delegateListener);

        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    protected DOMDataTreeChangeService delegate() {
        return delegate;
    }
}
