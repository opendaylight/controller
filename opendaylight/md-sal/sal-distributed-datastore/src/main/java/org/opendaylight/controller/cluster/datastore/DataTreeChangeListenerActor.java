/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Proxy actor which acts as a facade to the user-provided listener. Responsible for decapsulating
 * DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeChangeListenerActor extends AbstractDataTreeChangeListenerActor {
    private DataTreeChangeListenerActor(final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath) {
        super(listener, registeredPath);
    }

    static Props props(final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeChangeListenerActor.class, listener, registeredPath);
    }
}
