/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;

/**
 * Intermediate proxy registration returned to the user when we cannot
 * instantiate the registration immediately. It provides a bridge to
 * a real registration which may materialize at some point in the future.
 */
final class DelayedDataTreeListenerRegistration
        extends DelayedListenerRegistration<DOMDataTreeChangeListener, RegisterDataTreeChangeListener> {

    DelayedDataTreeListenerRegistration(final RegisterDataTreeChangeListener registerTreeChangeListener) {
        super(registerTreeChangeListener);
    }
}

