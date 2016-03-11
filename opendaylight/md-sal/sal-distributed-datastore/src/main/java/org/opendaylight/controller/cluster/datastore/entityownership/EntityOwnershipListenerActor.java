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
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An actor which is responsible for notifying an EntityOwnershipListener of changes.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipListenerActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(EntityOwnershipListenerActor.class);

    private final EntityOwnershipListener listener;

    private EntityOwnershipListenerActor(EntityOwnershipListener listener) {
        this.listener = listener;
    }

    @Override
    protected void handleReceive(Object message) {
        if (message instanceof EntityOwnershipChange) {
            onEntityOwnershipChanged((EntityOwnershipChange)message);
        } else {
            unknownMessage(message);
        }
    }

    private void onEntityOwnershipChanged(EntityOwnershipChange change) {
        LOG.debug("Notifying EntityOwnershipListener {}: {}", listener, change);

        try {
            listener.ownershipChanged(change);
        } catch (Exception e) {
            LOG.error("Error notifying listener {}", listener, e);
        }
    }

    static Props props(EntityOwnershipListener listener) {
        return Props.create(new EntityOwnershipListenerCreator(listener));
    }

    private static final class EntityOwnershipListenerCreator implements Creator<EntityOwnershipListenerActor> {
        private static final long serialVersionUID = 1L;

        private final EntityOwnershipListener listener;

        EntityOwnershipListenerCreator(EntityOwnershipListener listener) {
            this.listener = Preconditions.checkNotNull(listener);
        }

        @Override
        public EntityOwnershipListenerActor create() {
            return new EntityOwnershipListenerActor(listener);
        }
    }
}
