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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataChangeListener extends AbstractUntypedActor {
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
    private final SchemaContext schemaContext;
    private final YangInstanceIdentifier pathId;

    public DataChangeListener(SchemaContext schemaContext,
                              AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, YangInstanceIdentifier pathId) {
        this.listener = listener;
        this.schemaContext = schemaContext;
        this.pathId  = pathId;
    }

    @Override public void handleReceive(Object message) throws Exception {
        if(message.getClass().equals(DataChanged.SERIALIZABLE_CLASS)){
            DataChanged reply = DataChanged.fromSerialize(schemaContext,message, pathId);
            AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>>
                change = reply.getChange();
            this.listener.onDataChanged(change);

            if(getSender() != null){
                getSender().tell(new DataChangedReply().toSerializable(), getSelf());
            }

        }
    }

    public static Props props(final SchemaContext schemaContext, final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, final YangInstanceIdentifier pathId) {
        return Props.create(new Creator<DataChangeListener>() {
            @Override
            public DataChangeListener create() throws Exception {
                return new DataChangeListener(schemaContext,listener,pathId );
            }

        });

    }
}
