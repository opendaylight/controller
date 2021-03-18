/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.service;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

public class NativeEntityOwnershipListenerRegistration extends AbstractObjectRegistration<DOMEntityOwnershipListener>
        implements DOMEntityOwnershipListenerRegistration {

    private final String entityType;
    private final NativeEntityOwnershipService service;

    public NativeEntityOwnershipListenerRegistration(final DOMEntityOwnershipListener listener,
                                                     final String entityType,
                                                     final NativeEntityOwnershipService service) {
        super(listener);

        this.entityType = entityType;
        this.service = service;
    }

    @Override
    public @NonNull String getEntityType() {
        return entityType;
    }

    @Override
    protected void removeRegistration() {
        service.unregisterListener(entityType, getInstance());
    }

    @Override
    protected MoreObjects.ToStringHelper addToStringAttributes(final MoreObjects.ToStringHelper toStringHelper) {
        return toStringHelper.add("entityType", entityType);
    }
}
