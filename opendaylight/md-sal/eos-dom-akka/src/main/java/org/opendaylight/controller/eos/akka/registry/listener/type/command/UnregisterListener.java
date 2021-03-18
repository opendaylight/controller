/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type.command;

import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;

/**
 * Unregister a listener from the EntityTypeListenerRegistry.
 */
public class UnregisterListener implements TypeListenerRegistryCommand {

    private final String entityType;
    private final DOMEntityOwnershipListener delegateListener;

    public UnregisterListener(final String entityType, final DOMEntityOwnershipListener delegateListener) {
        this.entityType = entityType;
        this.delegateListener = delegateListener;
    }

    public String getEntityType() {
        return entityType;
    }

    public DOMEntityOwnershipListener getDelegateListener() {
        return delegateListener;
    }
}
