/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;

/**
 * An actor which is responsible for notifying an EntityOwnershipListener of changes.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipListenerActor extends AbstractUntypedActor {
    private final DOMEntityOwnershipListener listener;

    private EntityOwnershipListenerActor(DOMEntityOwnershipListener listener) {
        this.listener = listener;
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof DOMEntityOwnershipChange) {
            onEntityOwnershipChanged((DOMEntityOwnershipChange)message);
        } else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void onEntityOwnershipChanged(DOMEntityOwnershipChange change) {
        LOG.debug("Notifying EntityOwnershipListener {}: {}", listener, change);

        try {
            listener.ownershipChanged(change);
        } catch (Exception e) {
            LOG.error("Error notifying listener {}", listener, e);
        }
    }

    static Props props(DOMEntityOwnershipListener listener) {
        return Props.create(new EntityOwnershipListenerCreator(listener));
    }

    private static final class EntityOwnershipListenerCreator implements Creator<EntityOwnershipListenerActor> {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but we don't "
                + "create remote instances of this actor and thus don't need it to be Serializable.")
        private final DOMEntityOwnershipListener listener;

        EntityOwnershipListenerCreator(DOMEntityOwnershipListener listener) {
            this.listener = Preconditions.checkNotNull(listener);
        }

        @Override
        public EntityOwnershipListenerActor create() {
            return new EntityOwnershipListenerActor(listener);
        }
    }
}
