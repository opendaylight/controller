/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.dom.api.LeaderLocation;
import org.opendaylight.controller.cluster.dom.api.LeaderLocationListener;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;

/**
 * Proxy actor which acts as a facade for user-provided
 * {@link LeaderLocationListener}. It subscribes for {@link LeaderStateChanged}
 * notifications in its pre start hook and translates them to
 * {@link LeaderLocationListener#onLeaderLocationChanged(LeaderLocation)}
 * events.
 */
@Deprecated(forRemoval = true)
public final class RoleChangeListenerActor extends AbstractUntypedActor {
    private final LeaderLocationListener leaderLocationListener;
    private final ActorRef roleChangeNotifier;

    private RoleChangeListenerActor(final ActorRef roleChangeNotifier, final LeaderLocationListener listener) {
        this.roleChangeNotifier = requireNonNull(roleChangeNotifier);
        this.leaderLocationListener = requireNonNull(listener);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        roleChangeNotifier.tell(new RegisterRoleChangeListener(), getSelf());
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RoleChangeNotification) {
            ignoreMessage(message);
        } else if (message instanceof LeaderStateChanged) {
            onLeaderStateChanged((LeaderStateChanged) message);
        } else {
            unknownMessage(message);
        }
    }

    private void onLeaderStateChanged(final LeaderStateChanged message) {
        final LeaderLocation newLocation;
        if (message.getLeaderId() == null) {
            newLocation = LeaderLocation.UNKNOWN;
        } else if (message.getMemberId().equals(message.getLeaderId())) {
            newLocation = LeaderLocation.LOCAL;
        } else {
            newLocation = LeaderLocation.REMOTE;
        }

        // TODO should we wrap this in try catch block?
        leaderLocationListener.onLeaderLocationChanged(newLocation);
    }

    public static Props props(final ActorRef roleChangeNotifier, final LeaderLocationListener listener) {
        return Props.create(RoleChangeListenerActor.class, roleChangeNotifier, listener);
    }
}
