/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal implementation of a {@link DOMDataTreeChangeListener} which
 * encapsulates received notifications into a {@link DataTreeChanged}
 * message and forwards them towards the client's {@link DataTreeChangeListenerActor}.
 */
final class ForwardingDataTreeChangeListener implements DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingDataTreeChangeListener.class);

    private final ActorSelection actor;

    ForwardingDataTreeChangeListener(final ActorSelection actor) {
        this.actor = Preconditions.checkNotNull(actor, "actor should not be null");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        LOG.debug("Sending DataTreeChanged to {}", actor);
        actor.tell(new DataTreeChanged(changes), ActorRef.noSender());
    }

    @Override
    public String toString() {
        return "ForwardingDataTreeChangeListener [actor=" + actor + "]";
    }
}
