/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.GetInfo;
import org.opendaylight.controller.cluster.datastore.messages.OnInitialData;
import org.opendaylight.controller.cluster.mgmt.api.DataTreeListenerInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Proxy actor which acts as a facade to the user-provided listener. Responsible for decapsulating
 * DataTreeChanged messages and dispatching their context to the user.
 */
class DataTreeChangeListenerActor extends AbstractUntypedActor {
    private final DOMDataTreeChangeListener listener;
    private final YangInstanceIdentifier registeredPath;

    private boolean notificationsEnabled = false;
    private long notificationCount;
    private String logContext = "";

    DataTreeChangeListenerActor(final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath) {
        this.listener = requireNonNull(listener);
        this.registeredPath = requireNonNull(registeredPath);
    }

    @Override
    protected final void handleReceive(final Object message) {
        if (message instanceof DataTreeChanged) {
            dataTreeChanged((DataTreeChanged) message);
        } else if (message instanceof OnInitialData) {
            onInitialData((OnInitialData) message);
        } else if (message instanceof EnableNotification) {
            enableNotification((EnableNotification) message);
        } else if (message instanceof GetInfo) {
            getSender().tell(new DataTreeListenerInfo(listener.toString(), registeredPath.toString(),
                    notificationsEnabled, notificationCount), getSelf());
        } else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void onInitialData(final OnInitialData message) {
        LOG.debug("{}: Notifying onInitialData to listener {}", logContext, listener);

        try {
            this.listener.onInitialData();
        } catch (Exception e) {
            LOG.error("{}: Error notifying listener {}", logContext, this.listener, e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void dataTreeChanged(final DataTreeChanged message) {
        // Do nothing if notifications are not enabled
        if (!notificationsEnabled) {
            LOG.debug("{}: Notifications not enabled for listener {} - dropping change notification",
                    logContext, listener);
            return;
        }

        LOG.debug("{}: Sending {} change notification(s) {} to listener {}", logContext, message.getChanges().size(),
                message.getChanges(), listener);

        notificationCount++;

        try {
            this.listener.onDataTreeChanged(message.getChanges());
        } catch (Exception e) {
            LOG.error("{}: Error notifying listener {}", logContext, this.listener, e);
        }

        // TODO: do we really need this?
        // It seems the sender is never null but it doesn't hurt to check. If the caller passes in
        // a null sender (ActorRef.noSender()), akka translates that to the deadLetters actor.
        final ActorRef sender = getSender();
        if (sender != null && !sender.equals(getContext().system().deadLetters())) {
            sender.tell(DataTreeChangedReply.getInstance(), getSelf());
        }
    }

    private void enableNotification(final EnableNotification message) {
        logContext = message.getLogContext();
        notificationsEnabled = message.isEnabled();
        LOG.debug("{}: {} notifications for listener {}", logContext, notificationsEnabled ? "Enabled" : "Disabled",
                listener);
    }

    static Props props(final DOMDataTreeChangeListener listener, final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeChangeListenerActor.class, listener, registeredPath);
    }
}
