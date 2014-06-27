/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChangeListener extends AbstractUntypedActor {
    private final AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> listener;

    public DataChangeListener(
        AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> listener) {
        this.listener = listener;
    }

    @Override public void handleReceive(Object message) throws Exception {
        if(message instanceof DataChanged){
            DataChanged reply = (DataChanged) message;
            AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>
                change = reply.getChange();
            this.listener.onDataChanged(change);

            if(getSender() != null){
                getSender().tell(new DataChangedReply(), getSelf());
            }

        }
    }

    public static Props props(final AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> listener) {
        return Props.create(new Creator<DataChangeListener>() {
            @Override
            public DataChangeListener create() throws Exception {
                return new DataChangeListener(listener);
            }

        });

    }
}
