/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * An EntityOwnershipListenerRegistration records a request to register a ownership status change listener for a
 * given Entity. Calling close on the registration will unregister listeners and future ownership changes will not
 * be delivered to the listener.
 */
public interface EntityOwnershipListenerRegistration extends ObjectRegistration<EntityOwnershipListener> {

    /**
     * Return the entity type that the listener was registered for
     */
    @Nonnull String getEntityType();

    /**
     * Unregister the listener
     */
    @Override
    void close();
}
