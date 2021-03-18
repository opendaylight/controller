/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type.command;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;

public abstract class TypeListenerRegistryCommand {
    private final @NonNull String entityType;
    private final @NonNull DOMEntityOwnershipListener delegateListener;

    TypeListenerRegistryCommand(final String entityType, final DOMEntityOwnershipListener delegateListener) {
        this.entityType = requireNonNull(entityType);
        this.delegateListener = requireNonNull(delegateListener);
    }

    public final @NonNull String getEntityType() {
        return entityType;
    }

    public final @NonNull DOMEntityOwnershipListener getDelegateListener() {
        return delegateListener;
    }
}
