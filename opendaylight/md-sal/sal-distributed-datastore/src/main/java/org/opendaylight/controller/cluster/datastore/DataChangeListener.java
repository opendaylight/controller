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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Actor for a DataChangeListener.
 *
 * @deprecated Replaced by {@link DataTreeChangeListener}
 */
@Deprecated
public class DataChangeListener extends AbstractUntypedActor {
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener;
    private boolean notificationsEnabled = false;

    public DataChangeListener(AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener) {
        this.listener = Preconditions.checkNotNull(listener, "listener should not be null");
    }

    @Override
    public void handleReceive(Object message) {
        if (message instanceof DataChanged) {
            dataChanged(message);
        } else if (message instanceof EnableNotification) {
            enableNotification((EnableNotification) message);
        } else {
            unknownMessage(message);
        }
    }

    private void enableNotification(EnableNotification message) {
        notificationsEnabled = message.isEnabled();
        LOG.debug("{} notifications for listener {}", notificationsEnabled ? "Enabled" : "Disabled",
                listener);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void dataChanged(Object message) {

        // Do nothing if notifications are not enabled
        if (!notificationsEnabled) {
            LOG.debug("Notifications not enabled for listener {} - dropping change notification", listener);
            return;
        }

        DataChanged reply = (DataChanged) message;
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = reply.getChange();

        LOG.debug("Sending change notification {} to listener {}", change, listener);

        try {
            this.listener.onDataChanged(change);
        } catch (RuntimeException e) {
            LOG.error( String.format( "Error notifying listener %s", this.listener ), e );
        }

        if (isValidSender(getSender())) {
            getSender().tell(DataChangedReply.INSTANCE, getSelf());
        }
    }

    public static Props props(final AsyncDataChangeListener<YangInstanceIdentifier,
                                                            NormalizedNode<?, ?>> listener) {
        return Props.create(new DataChangeListenerCreator(listener));
    }

    private static class DataChangeListenerCreator implements Creator<DataChangeListener> {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but we don't "
                + "create remote instances of this actor and thus don't need it to be Serializable.")
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
