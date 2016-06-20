/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.Props;
import akka.japi.Creator;

/**
 * An actor which is responsible for notifying an DOMEntityOwnershipListener of changes.
 *
 */
public class DOMEntityOwnershipListenerActor extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(DOMEntityOwnershipListenerActor.class.getName());

    private final DOMEntityOwnershipListener listener;

    private DOMEntityOwnershipListenerActor(final DOMEntityOwnershipListener listener) {
        this.listener = listener;
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof DOMEntityOwnershipChange) {
            onEntityOwnershipChanged((DOMEntityOwnershipChange) message);
        } else {
            unknownMessage(message);
        }
    }

    private void onEntityOwnershipChanged(final DOMEntityOwnershipChange change) {
        LOG.debug("Notifying EntityOwnershipListener {}: {}", listener, change);

        try {
            listener.ownershipChanged(change);
        } catch (final Exception e) {
            LOG.error("Error notifying listener {}", listener, e);
        }
    }

    static Props props(final DOMEntityOwnershipListener listener) {
        return Props.create(new DOMEntityOwnershipListenerCreator(listener));
    }

    private static final class DOMEntityOwnershipListenerCreator implements Creator<DOMEntityOwnershipListenerActor> {
        private static final long serialVersionUID = 1L;

        private final DOMEntityOwnershipListener listener;

        DOMEntityOwnershipListenerCreator(final DOMEntityOwnershipListener listener) {
            this.listener = Preconditions.checkNotNull(listener);
        }

        @Override
        public DOMEntityOwnershipListenerActor create() {
            return new DOMEntityOwnershipListenerActor(listener);
        }
    }
}
