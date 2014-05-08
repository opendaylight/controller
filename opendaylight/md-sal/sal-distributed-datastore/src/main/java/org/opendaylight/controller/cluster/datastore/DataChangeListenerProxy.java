/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * DataChangeListenerProxy represents a single remote DataChangeListener
 */
public class DataChangeListenerProxy implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>{
    private final ActorSelection dataChangeListenerActor;

    public DataChangeListenerProxy(ActorSelection dataChangeListenerActor) {
        this.dataChangeListenerActor = dataChangeListenerActor;
    }

    @Override public void onDataChanged(
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
        dataChangeListenerActor.tell(new DataChanged(change), null);
    }
}
