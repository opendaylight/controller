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
 * Register a DOMEntityOwnershipListener for a given entity-type.
 */
public final class RegisterListener extends TypeListenerRegistryCommand {
    public RegisterListener(final String entityType, final DOMEntityOwnershipListener delegateListener) {
        super(entityType, delegateListener);
    }
}
