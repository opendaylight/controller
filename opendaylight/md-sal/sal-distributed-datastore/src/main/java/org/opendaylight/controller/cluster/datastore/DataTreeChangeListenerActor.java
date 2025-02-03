/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.GetInfo;
import org.opendaylight.controller.cluster.datastore.messages.OnInitialData;
import org.opendaylight.controller.cluster.mgmt.api.DataTreeListenerInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy actor which acts as a facade to the user-provided listener. Responsible for decapsulating
 * DataTreeChanged messages and dispatching their context to the user.
 */
class DataTreeChangeListenerActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeListenerActor.class);

    private final DOMDataTreeChangeListener listener;
    private final YangInstanceIdentifier registeredPath;

    private boolean notificationsEnabled = false;
    private long notificationCount;

    DataTreeChangeListenerActor(final String logName, final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath) {
        super(logName);
        this.listener = requireNonNull(listener);
        this.registeredPath = requireNonNull(registeredPath);
    }

    static Props props(final String logName, final DOMDataTreeChangeListener listener,
            final YangInstanceIdentifier registeredPath) {
        return Props.create(DataTreeChangeListenerActor.class, logName, listener, registeredPath);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    protected final void handleReceive(final Object message) {
        switch (message) {
            case DataTreeChanged msg -> dataTreeChanged(msg);
            case OnInitialData msg -> onInitialData(msg);
            case EnableNotification msg -> enableNotification(msg);
            case GetInfo msg -> {
                getSender().tell(new DataTreeListenerInfo(listener.toString(), registeredPath.toString(),
                    notificationsEnabled, notificationCount), self());
            }
            default -> unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void onInitialData(final OnInitialData message) {
        LOG.debug("{}: Notifying onInitialData to listener {}", logName, listener);
        try {
            listener.onInitialData();
        } catch (Exception e) {
            LOG.error("{}: Error notifying listener {}", logName, listener, e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void dataTreeChanged(final DataTreeChanged message) {
        // Do nothing if notifications are not enabled
        if (!notificationsEnabled) {
            LOG.debug("{}: Notifications not enabled for listener {} - dropping change notification",
                logName, listener);
            return;
        }

        final var changes = message.getChanges();
        LOG.debug("{}: Sending {} change notification(s) to listener {}", logName, changes.size(), listener);
        if (LOG.isTraceEnabled() && !changes.isEmpty()) {
            LOG.trace("{}: detailed change follow", logName);
            for (int i = 0, size = changes.size(); i < size; ++i) {
                LOG.trace("{}: change {}: {}", logName, i, changes.get(i));
            }
        }

        notificationCount++;

        try {
            listener.onDataTreeChanged(changes);
        } catch (Exception e) {
            LOG.error("{}: Error notifying listener {}", logName, listener, e);
        }

        // TODO: do we really need this?
        //       It seems the sender is never null but it doesn't hurt to check. If the caller passes in a null sender
        //       (ActorRef.noSender()), akka translates that to the deadLetters actor.
        // FIXME: yes, we want this, as DataTreeChanged should be a Request and we should be reporting at least a
        //        success, so that we have reliable DTCL delivery via TransmitQueue.
        final var sender = getSender();
        if (sender != null && !sender.equals(getContext().system().deadLetters())) {
            sender.tell(DataTreeChangedReply.getInstance(), self());
        }
    }

    private void enableNotification(final EnableNotification message) {
        notificationsEnabled = message.isEnabled();
        LOG.debug("{}: {} notifications for listener {}", logName, notificationsEnabled ? "Enabled" : "Disabled",
                listener);
    }
}
