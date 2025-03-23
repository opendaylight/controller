/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.serialization.Serialization;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;

/**
 * The RoleChangeNotifier is responsible for receiving Raft role and leader state change messages and notifying
 * the listeners (within the same node), which are registered with it.
 *
 * <p>The RoleChangeNotifier is instantiated by the Shard and injected into the RaftActor.
 */
public class RoleChangeNotifier extends AbstractUntypedActor implements AutoCloseable {
    private final Map<ActorPath, ActorRef> registeredListeners = new HashMap<>();
    private final String memberId;

    private RoleChangeNotification latestRoleChangeNotification = null;
    private LeaderStateChanged latestLeaderStateChanged;

    public RoleChangeNotifier(final String memberId) {
        this.memberId = memberId;
    }

    public static Props getProps(final String memberId) {
        return Props.create(RoleChangeNotifier.class, memberId);
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        LOG.info("RoleChangeNotifier:{} created and ready for shard:{}",
            Serialization.serializedActorPath(self()), memberId);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RegisterRoleChangeListener) {
            // register listeners for this shard

            ActorRef curRef = registeredListeners.get(getSender().path());
            if (curRef != null) {
                // ActorPaths would pass equal even if the unique id of the actors are different
                // if a listener actor is re-registering after reincarnation, then removing the existing
                // entry so the actor path with correct unique id is registered.
                registeredListeners.remove(getSender().path());
            }
            registeredListeners.put(getSender().path(), getSender());

            LOG.info("RoleChangeNotifier for {} , registered listener {}", memberId,
                getSender().path().toString());

            getSender().tell(new RegisterRoleChangeListenerReply(), self());

            if (latestLeaderStateChanged != null) {
                getSender().tell(latestLeaderStateChanged, self());
            }

            if (latestRoleChangeNotification != null) {
                getSender().tell(latestRoleChangeNotification, self());
            }


        } else if (message instanceof RoleChanged roleChanged) {
            // this message is sent by RaftActor. Notify registered listeners when this message is received.

            LOG.info("RoleChangeNotifier for {} , received role change from {} to {}", memberId,
                roleChanged.oldRole(), roleChanged.newRole());

            latestRoleChangeNotification = new RoleChangeNotification(roleChanged.memberId(), roleChanged.oldRole(),
                roleChanged.newRole());

            for (var listener : registeredListeners.values()) {
                listener.tell(latestRoleChangeNotification, self());
            }
        } else if (message instanceof LeaderStateChanged leaderStateChanged) {
            latestLeaderStateChanged = leaderStateChanged;

            for (ActorRef listener : registeredListeners.values()) {
                listener.tell(latestLeaderStateChanged, self());
            }
        } else {
            unknownMessage(message);
        }
    }

    @Override
    public void close() {
        registeredListeners.clear();
    }
}

