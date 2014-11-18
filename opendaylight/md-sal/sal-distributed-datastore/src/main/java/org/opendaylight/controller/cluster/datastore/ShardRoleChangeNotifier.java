/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Actor;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.notifications.ShardRoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.raft.base.messages.RaftRoleChanged;

/**
 * The ShardRoleChangeNotifier is responsible for receiving Raft role change messages and notifying
 * the listeners (within the same node), which are registered with it.
 * <p/>
 * The ShardRoleChangeNotifier is instantiated by the Shard and injected into the RaftActor.
 */
public class ShardRoleChangeNotifier extends AbstractUntypedActor implements AutoCloseable {

    private String memberId;
    private Map<ActorPath, ActorRef> registeredListeners = Maps.newHashMap();
    private ShardRoleChangeNotification latestRoleChangeNotification = null;

    public ShardRoleChangeNotifier(String memberId) {
        this.memberId = memberId;
    }

    public static Props getProps(final String memberId) {
        return Props.create(new Creator<Actor>() {
            @Override
            public Actor create() throws Exception {
                return new ShardRoleChangeNotifier(memberId);
            }
        });
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        LOG.info("ShardRoleChangeNotifier:{} created and ready for shard:{}",
            getSelf().path().toString(), memberId);
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
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

            LOG.info("ShardRoleChangeNotifier for {} , registered listener {}", memberId,
                getSender().path().toString());

            getSender().tell(new RegisterRoleChangeListenerReply(), getSelf());

            if (latestRoleChangeNotification != null) {
                getSender().tell(latestRoleChangeNotification, getSelf());
            }


        } else if (message instanceof RaftRoleChanged) {
            // this message is sent by RaftActor. Notify registered listeners when this message is received.
            RaftRoleChanged roleChanged = (RaftRoleChanged) message;

            LOG.info("ShardRoleChangeNotifier for {} , received role change from {} to {}", memberId,
                roleChanged.getOldRole().name(), roleChanged.getNewRole().name());


            latestRoleChangeNotification =
                new ShardRoleChangeNotification(roleChanged.getMemberId(),
                    roleChanged.getOldRole().name(), roleChanged.getNewRole().name());

            for (ActorRef listener: registeredListeners.values()) {
                listener.tell(latestRoleChangeNotification, getSelf());
            }
        }
    }

    @Override
    public void close() throws Exception {
        registeredListeners.clear();
    }
}

