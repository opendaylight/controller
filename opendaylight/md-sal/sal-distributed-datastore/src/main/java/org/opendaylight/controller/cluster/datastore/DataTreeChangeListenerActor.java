/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Proxy actor which acts as a facade to the user-provided listener. Responsible for decapsulating
 * DataTreeChanged messages and dispatching their context to the user.
 */
final class DataTreeChangeListenerActor extends AbstractUntypedActor {
    private final DOMDataTreeChangeListener listener;
    private final YangInstanceIdentifier registeredPath;
    private boolean notificationsEnabled = false;
    private String logContext = "";

    private DataTreeChangeListenerActor(final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath) {
        this.listener = Preconditions.checkNotNull(listener);
        this.registeredPath = Preconditions.checkNotNull(registeredPath);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof DataTreeChanged) {
            dataChanged((DataTreeChanged)message);
        } else if (message instanceof EnableNotification) {
            enableNotification((EnableNotification) message);
        } else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void dataChanged(final DataTreeChanged message) {
        // Do nothing if notifications are not enabled
        if (!notificationsEnabled) {
            LOG.debug("{}: Notifications not enabled for listener {} - dropping change notification",
                    logContext, listener);
            return;
        }

        LOG.debug("{}: Sending {} change notification(s) {} to listener {}", logContext, message.getChanges().size(),
                message.getChanges(), listener);

        try {
            this.listener.onDataTreeChanged(message.getChanges());
        } catch (Exception e) {
            LOG.error("{}: Error notifying listener {}", logContext, this.listener, e);
        }

        // TODO: do we really need this?
        // It seems the sender is never null but it doesn't hurt to check. If the caller passes in
        // a null sender (ActorRef.noSender()), akka translates that to the deadLetters actor.
        if (getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(DataTreeChangedReply.getInstance(), getSelf());
        }
    }

    private void enableNotification(final EnableNotification message) {
        logContext = message.getLogContext();
        notificationsEnabled = message.isEnabled();
        LOG.debug("{}: {} notifications for listener {}", logContext, notificationsEnabled ? "Enabled" : "Disabled",
                listener);
    }

    public static Props props(final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeChangeListenerActor.class, listener, registeredPath);
    }
}
