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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataChangeListener extends AbstractUntypedActor {
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
    private final SchemaContext schemaContext;
    private final YangInstanceIdentifier pathId;
    private boolean notificationsEnabled = false;

    public DataChangeListener(SchemaContext schemaContext,
                              AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener, YangInstanceIdentifier pathId) {

        this.schemaContext = Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
        this.listener = Preconditions.checkNotNull(listener, "listener should not be null");
        this.pathId  = Preconditions.checkNotNull(pathId, "pathId should not be null");
    }

    @Override public void handleReceive(Object message) throws Exception {
        if(message instanceof DataChanged){
            dataChanged(message);
        } else if(message instanceof EnableNotification){
            enableNotification((EnableNotification) message);
        }
    }

    private void enableNotification(EnableNotification message) {
        notificationsEnabled = message.isEnabled();
    }

    private void dataChanged(Object message) {

        // Do nothing if notifications are not enabled
        if(!notificationsEnabled){
            return;
        }

        DataChanged reply = (DataChanged) message;
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>>
            change = reply.getChange();
        this.listener.onDataChanged(change);

        if(getSender() != null){
            getSender().tell(new DataChangedReply(), getSelf());
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
