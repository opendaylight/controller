/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Actor that generates data change notifications and publishes them to listeners. This is used to offload
 * the potentially expensive change notification generation from the Shard actor.
 *
 * @author Thomas Pantelis
 */
public class ShardDataTreeChangeListenerNotifierActor extends AbstractUntypedActor {
    private final ShardDataTreeChangeListenerNotifier delegateNotifier;

    public ShardDataTreeChangeListenerNotifierActor(ShardDataTreeChangeListenerNotifier delegateNotifier) {
        this.delegateNotifier = delegateNotifier;
    }

    @Override
    protected void handleReceive(Object message) {
        if(message instanceof DataTreeCandidate) {
            delegateNotifier.notifyListeners((DataTreeCandidate)message);
        }
    }

    public static Props props(final ShardDataTreeChangeListenerNotifier delegateNotifier) {
        return Props.create(ShardDataTreeChangeListenerNotifierActor.class, delegateNotifier);
    }
}
