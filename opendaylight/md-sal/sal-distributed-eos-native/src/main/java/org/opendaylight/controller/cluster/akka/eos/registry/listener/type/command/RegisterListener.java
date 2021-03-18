/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command;

import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;

/**
 * Register a DOMEntityOwnershipListener for a given entity-type.
 */
public class RegisterListener implements TypeListenerRegistryCommand {

    private final String entityType;
    private final DOMEntityOwnershipListener delegateListener;

    public RegisterListener(final String entityType, final DOMEntityOwnershipListener delegateListener) {
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
