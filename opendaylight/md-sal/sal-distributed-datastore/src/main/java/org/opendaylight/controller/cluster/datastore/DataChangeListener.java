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

public class DataChangeListener extends AbstractUntypedActor {
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
    private volatile boolean notificationsEnabled = false;

    public DataChangeListener(AsyncDataChangeListener<YangInstanceIdentifier,
                                                      NormalizedNode<?, ?>> listener) {
        this.listener = Preconditions.checkNotNull(listener, "listener should not be null");
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

    public static Props props(final AsyncDataChangeListener<YangInstanceIdentifier,
                                                            NormalizedNode<?, ?>> listener) {
        return Props.create(new DataChangeListenerCreator(listener));
    }

    private static class DataChangeListenerCreator implements Creator<DataChangeListener> {
        private static final long serialVersionUID = 1L;

        final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;

        DataChangeListenerCreator(
                AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener) {
            this.listener = listener;
        }

        @Override
        public DataChangeListener create() throws Exception {
            return new DataChangeListener(listener);
        }
    }
}
