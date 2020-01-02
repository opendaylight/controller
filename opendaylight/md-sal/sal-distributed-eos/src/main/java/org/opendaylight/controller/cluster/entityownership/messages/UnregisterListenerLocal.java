/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.messages;

import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;

/**
 * Message sent to the local EntityOwnershipShard to unregister an EntityOwnershipListener.
 *
 * @author Thomas Pantelis
 */
public class UnregisterListenerLocal {
    private final DOMEntityOwnershipListener listener;
    private final String entityType;

    public UnregisterListenerLocal(final DOMEntityOwnershipListener listener, final String entityType) {
        this.listener = listener;
        this.entityType = entityType;
    }

    public DOMEntityOwnershipListener getListener() {
        return listener;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "UnregisterListenerLocal [entityType=" + entityType + ", listener=" + listener + "]";
    }
}
