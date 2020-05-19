/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.OnInitialData;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class RootDataTreeChangeListenerActor extends AbstractDataTreeChangeListenerActor {
    private final ActorRef defaultActor;
    private final int shardCount;

    private Map<ActorRef, Object> initialMessages = new HashMap<>();
    private List<Object> otherMessages = new ArrayList<>();

    private RootDataTreeChangeListenerActor(final DOMDataTreeChangeListener listener, final ActorRef defaultActor,
            final int shardCount) {
        super(listener, YangInstanceIdentifier.empty());
        this.defaultActor = requireNonNull(defaultActor);
        this.shardCount = shardCount;
    }

    @Override
    void onInitialData(final OnInitialData message) {
        final ActorRef sender = getSender();
        verifyNotNull(initialMessages, "Receinved OnInitialData from %s after initial convergence", sender);

        final Object prev = initialMessages.put(sender, message);
        verify(prev == null, "Received OnInitialData from %s after %s", sender, prev);
        checkInitialConvergence();
    }

    @Override
    void dataTreeChanged(final DataTreeChanged message) {
        if (initialMessages == null) {
            super.dataTreeChanged(message);
        } else {
            processMessage(message);
        }
    }

    private void processMessage(final Object message) {
        // Put the message into initial messages if we do not have a message from that actor yet. If we do, just stash
        // it to other messages for later processing.
        if (initialMessages.putIfAbsent(getSender(), message) == null) {
            checkInitialConvergence();
        } else {
            otherMessages.add(message);
        }
    }

    private void checkInitialConvergence() {
        if (initialMessages.size() != shardCount) {
            // We do not have initial state from all shards yet
            return;
        }

        // FIXME: construct initial notification

        initialMessages = null;
        otherMessages = null;
    }

    static Props props(final DOMDataTreeChangeListener instance, final ActorRef defaultActor, final int shardCount) {
        return Props.create(RootDataTreeChangeListenerActor.class, defaultActor, shardCount);
    }
}
